/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.questionbank.extraction.service;

import com.examplatform.questionbank.ai.generation.ModelRouter;
import com.examplatform.questionbank.domain.Subject;
import com.examplatform.questionbank.domain.Subtopic;
import com.examplatform.questionbank.domain.Topic;
import com.examplatform.questionbank.dto.QuestionOption;
import com.examplatform.questionbank.extraction.ai.LiteLlmPageExtractionClient;
import com.examplatform.questionbank.extraction.ai.LlmPageExtractionResult;
import com.examplatform.questionbank.extraction.ai.LlmQuotaExhaustedException;
import com.examplatform.questionbank.extraction.ai.PdfPageContent;
import com.examplatform.questionbank.extraction.ai.QuestionPaperPdfParser;
import com.examplatform.questionbank.extraction.client.AssetServiceClient;
import com.examplatform.questionbank.extraction.domain.ExtractedQuestion;
import com.examplatform.questionbank.extraction.domain.ExtractedQuestionStatus;
import com.examplatform.questionbank.extraction.domain.PaperExtractionStatus;
import com.examplatform.questionbank.extraction.domain.QuestionPaperMetadata;
import com.examplatform.questionbank.extraction.repository.ExtractedQuestionRepository;
import com.examplatform.questionbank.extraction.repository.QuestionPaperMetadataRepository;
import com.examplatform.questionbank.repository.SubjectRepository;
import com.examplatform.questionbank.repository.SubtopicRepository;
import com.examplatform.questionbank.repository.TopicRepository;
import com.examplatform.shared.constants.QuestionTaxonomyConstants;
import com.examplatform.shared.constants.QuestionTypeConstants;
import com.examplatform.shared.tenant.TenantContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Asynchronous batch worker executing page-by-page extraction of Question Paper PDFs.
 * Coordinates PDFBox text/vision extraction, LiteLLM / deterministic fallback inference,
 * cover page metadata auto-enrichment, and ExtractedQuestion staging persistence.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncPaperExtractionWorker {

    private final QuestionPaperMetadataRepository metadataRepository;
    private final ExtractedQuestionRepository extractedQuestionRepository;
    private final AssetServiceClient assetServiceClient;
    private final QuestionPaperPdfParser pdfParser;
    private final LiteLlmPageExtractionClient extractionClient;
    private final ModelRouter modelRouter;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final SubtopicRepository subtopicRepository;
    private final ObjectMapper objectMapper;

    private final Set<UUID> activeJobIds = ConcurrentHashMap.newKeySet();
    private final Set<UUID> cancelledJobIds = ConcurrentHashMap.newKeySet();

    /**
     * Flags a running paper extraction job for graceful cancellation before the next page.
     *
     * @param paperId paper metadata ID to cancel
     */
    public void requestCancel(UUID paperId) {
        if (paperId != null) {
            cancelledJobIds.add(paperId);
            log.info("Extraction cancellation requested for paperId={}", paperId);
        }
    }

    /**
     * Asynchronous execution worker for a single uploaded Question Paper PDF.
     *
     * @param paperId identifier of the QuestionPaperMetadata record
     * @param tenantId examination authority identifier
     */
    @Async
    public void processPaperExtractionJob(UUID paperId, String tenantId) {
        if (!activeJobIds.add(paperId)) {
            log.warn("Paper extraction job {} is already active. Ignoring duplicate invocation.", paperId);
            return;
        }

        try {
            TenantContext.setTenantId(tenantId);
            executeJob(paperId, tenantId);
        } catch (Exception e) {
            log.error("Fatal error during paper extraction job {}: {}", paperId, e.getMessage(), e);
            handleJobLevelFailure(paperId, tenantId, e.getMessage());
        } finally {
            TenantContext.clear();
            activeJobIds.remove(paperId);
            cancelledJobIds.remove(paperId);
        }
    }

    private void executeJob(UUID paperId, String tenantId) throws IOException {
        Optional<QuestionPaperMetadata> paperOpt = metadataRepository.findByIdAndTenantId(paperId, tenantId);
        if (paperOpt.isEmpty()) {
            log.warn("QuestionPaperMetadata not found for paperId={} and tenantId={}", paperId, tenantId);
            return;
        }

        QuestionPaperMetadata paper = paperOpt.get();
        paper.setExtractionStatus(PaperExtractionStatus.PROCESSING);
        paper.setStartedAt(Instant.now());
        String extractionModel = modelRouter.selectPaperExtractionModel(true);
        paper.setModelUsed(extractionModel);
        paper = metadataRepository.save(paper);

        UUID assetId = paper.getFileAssetId();
        if (assetId == null) {
            throw new IllegalStateException("Paper " + paperId + " has no associated fileAssetId");
        }
        byte[] pdfBytes = assetServiceClient.downloadAssetBytes(assetId, tenantId);
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalStateException("Failed to download PDF bytes from asset-service for assetId: " + assetId);
        }

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            int totalPages = document.getNumberOfPages();
            paper.setTotalPages(totalPages);
            paper = metadataRepository.save(paper);

            log.info("Starting extraction for paperId={} ('{}'), totalPages={}",
                    paperId, paper.getExaminationName(), totalPages);

            int nextSequentialQuestionNumber = calculateNextSequentialNumber(paperId, tenantId);
            boolean cancelled = false;
            boolean quotaExhausted = false;
            ActivePassage activePassage = null;

            List<Integer> selectedPages = QuestionPaperExtractionService.parsePageSelection(paper.getPageSelection(), totalPages);
            List<ExtractedQuestion> existingQuestions = extractedQuestionRepository
                    .findByPaperMetadataIdAndTenantIdOrderByQuestionNumberAsc(paperId, tenantId);
            Set<Integer> pagesWithQuestions = existingQuestions.stream()
                    .map(ExtractedQuestion::getSourcePageNumber)
                    .filter(Objects::nonNull)
                    .collect(java.util.stream.Collectors.toSet());

            Set<Integer> failedPagesSet = paper.getFailedPages().stream()
                    .map(m -> (Number) m.get("pageNumber"))
                    .filter(Objects::nonNull)
                    .map(Number::intValue)
                    .collect(java.util.stream.Collectors.toSet());

            List<Integer> pagesToProcess = QuestionPaperExtractionService.computeResumePages(
                    selectedPages, pagesWithQuestions, failedPagesSet);

            for (int page : pagesToProcess) {
                if (cancelledJobIds.contains(paperId)) {
                    log.info("Extraction job {} detected cancellation flag at page {}/{}", paperId, page, totalPages);
                    markPaperCancelled(paper);
                    cancelled = true;
                    break;
                }

                QuestionPaperMetadata currentCheck = metadataRepository.findByIdAndTenantId(paperId, tenantId).orElse(paper);
                if (currentCheck.getExtractionStatus() == PaperExtractionStatus.CANCELLED) {
                    log.info("Extraction job {} database status is CANCELLED at page {}/{}. Halting.", paperId, page, totalPages);
                    cancelled = true;
                    break;
                }

                removeFailedPage(paper, page);
                extractedQuestionRepository.deleteByPaperMetadataIdAndSourcePageNumber(paperId, page);

                try {
                    PdfPageContent pageContent = pdfParser.extractSinglePage(document, page);
                    String examContextHint = buildExamContextHint(paper);
                    LlmPageExtractionResult result = extractionClient.extractPage(pageContent, examContextHint);

                    if (page == 1 && (result.isCoverPage() || hasMetadataSignals(result))) {
                        enrichPaperMetadataFromCover(paper, result);
                    }

                    List<LlmPageExtractionResult.ExtractedItemPayload> questions = result.getQuestions();
                    if (questions != null && !questions.isEmpty()) {
                        for (LlmPageExtractionResult.ExtractedItemPayload item : questions) {
                            int qNum;
                            if (item.getQuestionNumber() != null && item.getQuestionNumber() > 0) {
                                qNum = item.getQuestionNumber();
                                if (qNum >= nextSequentialQuestionNumber) {
                                    nextSequentialQuestionNumber = qNum + 1;
                                }
                            } else {
                                qNum = nextSequentialQuestionNumber++;
                            }

                            if (activePassage != null && qNum > activePassage.getRangeEnd()) {
                                activePassage = null;
                            }

                            if (item.getPassageText() != null && !item.getPassageText().isBlank()
                                    && item.getPassageRangeEnd() != null && item.getPassageRangeEnd() >= qNum) {
                                String hindiPassage = null;
                                if (item.getHindiContent() != null && !item.getHindiContent().isBlank()
                                        && item.getPassageRangeStart() != null) {
                                    hindiPassage = item.getHindiContent();
                                }
                                activePassage = new ActivePassage(item.getPassageText(), hindiPassage, item.getPassageRangeEnd());
                            } else if ((item.getPassageText() == null || item.getPassageText().isBlank())
                                    && activePassage != null && qNum <= activePassage.getRangeEnd()) {
                                item.setPassageText(activePassage.getPassageText());
                            }

                            upsertExtractedQuestion(paper, page, pageContent, item, qNum, tenantId);
                        }
                    }

                    paper.setLastCompletedPage(page);
                    paper.setPagesProcessed(page);
                    paper.setQuestionsExtracted((int) extractedQuestionRepository.countByPaperMetadataIdAndTenantId(paperId, tenantId));
                    paper = metadataRepository.save(paper);
                } catch (LlmQuotaExhaustedException qex) {
                    log.warn("LLM quota exhausted on page {} for paperId={}: {}",
                            qex.getPageNumber(), paperId, qex.getMessage());
                    paper.setExtractionStatus(PaperExtractionStatus.QUOTA_EXHAUSTED);
                    paper.setErrorMessage("Upstream LLM quota exhausted on page " + qex.getPageNumber() + ". Extraction paused and can be resumed.");
                    paper.setCompletedAt(Instant.now());
                    paper = metadataRepository.save(paper);
                    quotaExhausted = true;
                    break;
                } catch (Exception ex) {
                    log.warn("Failed to extract page {}/{} for paperId={}: {}", page, totalPages, paperId, ex.getMessage(), ex);
                    Map<String, Object> errorRecord = new HashMap<>();
                    errorRecord.put("pageNumber", page);
                    errorRecord.put("error", ex.getMessage() != null ? ex.getMessage() : "Extraction failed");
                    errorRecord.put("timestamp", Instant.now().toString());
                    paper.getFailedPages().add(errorRecord);
                    paper.setPagesProcessed(page);
                    paper = metadataRepository.save(paper);
                }
            }

            if (!cancelled && !quotaExhausted) {
                finalizeJobStatus(paper, totalPages, selectedPages);
            }
        }
    }

    private void finalizeJobStatus(QuestionPaperMetadata paper, int totalPages, List<Integer> selectedPages) {
        List<Map<String, Object>> failedPages = paper.getFailedPages();
        int targetTotal = (selectedPages != null && !selectedPages.isEmpty()) ? selectedPages.size() : totalPages;
        if (failedPages == null || failedPages.isEmpty()) {
            paper.setExtractionStatus(PaperExtractionStatus.COMPLETED);
        } else if (failedPages.size() < targetTotal) {
            paper.setExtractionStatus(PaperExtractionStatus.PARTIAL_FAILED);
        } else {
            paper.setExtractionStatus(PaperExtractionStatus.FAILED);
            paper.setErrorMessage("All pages failed during extraction");
        }
        paper.setCompletedAt(Instant.now());
        metadataRepository.save(paper);
        log.info("Finished extraction job for paperId={}, finalStatus={}", paper.getId(), paper.getExtractionStatus());
    }

    private void removeFailedPage(QuestionPaperMetadata paper, int pageNumber) {
        if (paper.getFailedPages() != null) {
            paper.getFailedPages().removeIf(m -> {
                Object p = m.get("pageNumber");
                return p instanceof Number && ((Number) p).intValue() == pageNumber;
            });
        }
    }

    private void markPaperCancelled(QuestionPaperMetadata paper) {
        paper.setExtractionStatus(PaperExtractionStatus.CANCELLED);
        paper.setCompletedAt(Instant.now());
        metadataRepository.save(paper);
    }

    private void handleJobLevelFailure(UUID paperId, String tenantId, String errorMessage) {
        try {
            Optional<QuestionPaperMetadata> paperOpt = metadataRepository.findByIdAndTenantId(paperId, tenantId);
            if (paperOpt.isPresent()) {
                QuestionPaperMetadata paper = paperOpt.get();
                paper.setExtractionStatus(PaperExtractionStatus.FAILED);
                paper.setErrorMessage(errorMessage);
                paper.setCompletedAt(Instant.now());
                metadataRepository.save(paper);
            }
        } catch (Exception e) {
            log.error("Failed to mark paper {} as FAILED: {}", paperId, e.getMessage(), e);
        }
    }

    private int calculateNextSequentialNumber(UUID paperId, String tenantId) {
        List<ExtractedQuestion> existing = extractedQuestionRepository
                .findByPaperMetadataIdAndTenantIdOrderByQuestionNumberAsc(paperId, tenantId);
        return existing.stream()
                .map(ExtractedQuestion::getQuestionNumber)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private String buildExamContextHint(QuestionPaperMetadata paper) {
        StringBuilder hint = new StringBuilder();
        if (paper.getExaminationName() != null) {
            hint.append(paper.getExaminationName());
        }
        if (paper.getExamYear() != null) {
            hint.append(' ').append(paper.getExamYear());
        }
        if (paper.getSubjectName() != null) {
            hint.append(" - ").append(paper.getSubjectName());
        }
        return hint.toString();
    }

    private boolean hasMetadataSignals(LlmPageExtractionResult result) {
        return result.getDetectedPaperCode() != null
                || result.getDetectedBookletSerialNo() != null
                || result.getDetectedSubjectName() != null
                || result.getDetectedTotalQuestions() != null
                || result.getDetectedDurationMinutes() != null
                || result.getDetectedMaxMarks() != null
                || result.getDetectedNegativeMarkingRatio() != null
                || (result.getDetectedSections() != null && !result.getDetectedSections().isEmpty());
    }

    private void enrichPaperMetadataFromCover(QuestionPaperMetadata paper, LlmPageExtractionResult result) {
        if (paper.getPaperCode() == null && result.getDetectedPaperCode() != null) {
            paper.setPaperCode(result.getDetectedPaperCode());
        }
        if (paper.getBookletSerialNo() == null && result.getDetectedBookletSerialNo() != null) {
            paper.setBookletSerialNo(result.getDetectedBookletSerialNo());
        }
        if (paper.getSubjectName() == null && result.getDetectedSubjectName() != null) {
            paper.setSubjectName(result.getDetectedSubjectName());
        }
        if (paper.getTotalQuestionsExpected() == 0 && result.getDetectedTotalQuestions() != null) {
            paper.setTotalQuestionsExpected(result.getDetectedTotalQuestions());
        }
        if (paper.getDurationMinutes() == null && result.getDetectedDurationMinutes() != null) {
            paper.setDurationMinutes(result.getDetectedDurationMinutes());
        }
        if (paper.getMaxMarks() == null && result.getDetectedMaxMarks() != null) {
            paper.setMaxMarks(result.getDetectedMaxMarks());
        }
        if (paper.getNegativeMarkingRatio() == null && result.getDetectedNegativeMarkingRatio() != null) {
            paper.setNegativeMarkingRatio(result.getDetectedNegativeMarkingRatio());
        }
        if ((paper.getSectionsMetadata() == null || paper.getSectionsMetadata().isEmpty())
                && result.getDetectedSections() != null && !result.getDetectedSections().isEmpty()) {
            paper.setSectionsMetadata(result.getDetectedSections());
        }
    }

    private void upsertExtractedQuestion(
            QuestionPaperMetadata paper,
            int page,
            PdfPageContent pageContent,
            LlmPageExtractionResult.ExtractedItemPayload item,
            int questionNumber,
            String tenantId) {

        Optional<ExtractedQuestion> existingOpt = extractedQuestionRepository
                .findByPaperMetadataIdAndQuestionNumber(paper.getId(), questionNumber);

        ExtractedQuestion eq = existingOpt.orElseGet(() -> {
            ExtractedQuestion created = new ExtractedQuestion();
            created.setPaperMetadataId(paper.getId());
            created.setTenantId(tenantId);
            created.setQuestionNumber(questionNumber);
            return created;
        });

        eq.setSourcePageNumber(page);
        eq.setSectionName(item.getSectionName());

        String questionType = item.getQuestionType();
        if (questionType == null || questionType.isBlank()) {
            questionType = QuestionTypeConstants.SINGLE_MCQ;
        }
        eq.setQuestionType(questionType);

        String normalizedContent = pdfParser.normalizeMathAndFormatting(item.getContent());
        eq.setContent(normalizedContent != null ? normalizedContent : "");

        List<QuestionOption> options = item.getOptions() != null ? new ArrayList<>(item.getOptions()) : new ArrayList<>();
        String[] optionIds = {"A", "B", "C", "D", "E", "F"};
        for (int i = 0; i < options.size(); i++) {
            QuestionOption opt = options.get(i);
            if (opt.getId() == null || opt.getId().isBlank()) {
                opt.setId(i < optionIds.length ? optionIds[i] : String.valueOf((char) ('A' + i)));
            }
            if (opt.getText() != null) {
                opt.setText(pdfParser.normalizeMathAndFormatting(opt.getText()));
            }
            if (item.getAnswerKey() != null && !item.getAnswerKey().isBlank()) {
                opt.setCorrect(opt.getId().trim().equalsIgnoreCase(item.getAnswerKey().trim()));
            }
        }
        eq.setOptions(options);

        eq.setAnswerKey(item.getAnswerKey());
        eq.setExplanation(pdfParser.normalizeMathAndFormatting(item.getExplanation()));
        eq.setPassageText(pdfParser.normalizeMathAndFormatting(item.getPassageText()));

        // Bilingual Hindi translations
        Map<String, Map<String, Object>> translations = new HashMap<>();
        boolean hasHindiContent = item.getHindiContent() != null && !item.getHindiContent().isBlank();
        boolean hasHindiOptions = item.getHindiOptions() != null && !item.getHindiOptions().isEmpty();
        if (hasHindiContent || hasHindiOptions) {
            Map<String, Object> hiPayload = new HashMap<>();
            hiPayload.put("content", item.getHindiContent() != null
                    ? pdfParser.normalizeMathAndFormatting(item.getHindiContent()) : "");
            hiPayload.put("options", item.getHindiOptions() != null ? item.getHindiOptions() : List.of());
            translations.put("hi", hiPayload);
        }
        eq.setTranslations(translations);

        List<String> mediaUrls = new ArrayList<>();
        if (item.isHasDiagram() && pageContent.getEmbeddedDiagramDataUris() != null) {
            mediaUrls.addAll(pageContent.getEmbeddedDiagramDataUris());
        }
        if (item.getCroppedDiagramDataUris() != null && !item.getCroppedDiagramDataUris().isEmpty()) {
            mediaUrls.addAll(item.getCroppedDiagramDataUris());
        }
        eq.setMediaUrls(mediaUrls);

        boolean hasImages = item.isHasDiagram()
                || (pageContent.getEmbeddedDiagramDataUris() != null && !pageContent.getEmbeddedDiagramDataUris().isEmpty())
                || (item.getCroppedDiagramDataUris() != null && !item.getCroppedDiagramDataUris().isEmpty())
                || !mediaUrls.isEmpty();
        eq.setHasImages(hasImages);
        eq.setHasTables(item.isHasTables());
        eq.setHasFormulas(item.isHasFormulas());

        // Resolve taxonomy defaults
        Long initialSubjectId = paper.getDefaultSubjectId();
        if (initialSubjectId == null && paper.getSubjectName() != null && !paper.getSubjectName().isBlank()) {
            initialSubjectId = resolveSubjectIdByName(paper.getSubjectName(), tenantId);
        }
        final Long resolvedSubjectId = initialSubjectId;
        eq.setSuggestedSubjectId(resolvedSubjectId);

        Long initialTopicId = paper.getDefaultTopicId();
        if (initialTopicId == null && resolvedSubjectId != null) {
            initialTopicId = topicRepository.findBySubjectIdAndTenantId(resolvedSubjectId, tenantId).stream()
                    .findFirst()
                    .map(Topic::getId)
                    .orElseGet(() -> topicRepository.findBySubjectIdAndTenantIdIsNull(resolvedSubjectId).stream()
                            .findFirst()
                            .map(Topic::getId)
                            .orElse(null));
        }
        final Long resolvedTopicId = initialTopicId;
        eq.setSuggestedTopicId(resolvedTopicId);

        Long initialSubtopicId = paper.getDefaultSubtopicId();
        if (initialSubtopicId == null && resolvedTopicId != null) {
            initialSubtopicId = subtopicRepository.findByTopicIdAndTenantId(resolvedTopicId, tenantId).stream()
                    .findFirst()
                    .map(Subtopic::getId)
                    .orElseGet(() -> subtopicRepository.findByTopicIdAndTenantIdIsNull(resolvedTopicId).stream()
                            .findFirst()
                            .map(Subtopic::getId)
                            .orElse(null));
        }
        eq.setSuggestedSubtopicId(initialSubtopicId);

        eq.setSuggestedDifficulty(item.getSuggestedDifficulty() != null
                ? item.getSuggestedDifficulty() : QuestionTaxonomyConstants.Difficulty.MEDIUM);
        eq.setSuggestedCognitiveLevel(item.getSuggestedCognitiveLevel() != null
                ? item.getSuggestedCognitiveLevel() : QuestionTaxonomyConstants.CognitiveLevel.UNDERSTAND);
        eq.setExtractionConfidence(item.getConfidence() != null ? item.getConfidence() : 0.85);
        eq.setParsingTier(pageContent.getParsingTier() != null ? pageContent.getParsingTier() : PdfPageContent.TIER_0_LOCAL_TEXT);
        eq.setStatus(ExtractedQuestionStatus.EXTRACTED);

        try {
            Map<String, Object> rawMap = objectMapper.convertValue(item, new TypeReference<Map<String, Object>>() {});
            eq.setRawLlmPayload(rawMap);
        } catch (Exception ex) {
            log.debug("Could not serialize raw LLM item payload for question {}: {}", questionNumber, ex.getMessage());
        }

        extractedQuestionRepository.save(eq);
    }

    private Long resolveSubjectIdByName(String name, String tenantId) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return subjectRepository.findByNameAndTenantId(name.trim(), tenantId)
                .or(() -> subjectRepository.findByNameAndTenantIdIsNull(name.trim()))
                .map(Subject::getId)
                .orElse(null);
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class ActivePassage {
        private String passageText;
        private String hindiPassageText;
        private int rangeEnd;
    }
}
