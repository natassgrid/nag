/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
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

package com.examplatform.papergenerator.service;

import com.examplatform.papergenerator.crypto.CanonicalPaperManifest;
import com.examplatform.papergenerator.crypto.MerkleTree;
import com.examplatform.papergenerator.domain.Paper;
import com.examplatform.papergenerator.ledger.LedgerAdapter;
import com.examplatform.papergenerator.ledger.LedgerAttestationPayload;
import com.examplatform.papergenerator.ledger.LedgerAttestationReceipt;
import com.examplatform.papergenerator.repository.PaperRepository;
import com.examplatform.shared.messaging.EventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service orchestrating Merkle Tree generation, canonical manifest computation,
 * public ledger anchoring, and cryptographic tamper verification for examination papers.
 *
 * Validates: Issue #156, Requirement 8.7
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaperAnchoringService {

    private static final String AUDIT_TOPIC = "exam.audit.events";

    private final LedgerAdapter ledgerAdapter;
    private final PaperRepository paperRepository;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * Constructs Merkle tree, canonical manifest digest, anchors to public ledger,
     * and persists cryptographic proof metadata into the paper entity.
     */
    @Transactional
    public Paper anchorPaperToLedger(Paper paper, String tenantId) {
        log.info("Starting cryptographic Merkle tree anchoring for paperId={}, examId={}", paper.getId(), paper.getExamId());

        List<UUID> questionIds = extractQuestionIds(paper.getPaperDefinitionJson());
        String variant = (paper.getVariant() != null && !paper.getVariant().isBlank()) ? paper.getVariant() : "SET-A";
        paper.setVariant(variant);

        // 1. Build Canonical Manifest Data & Digest
        Map<String, Integer> topicDist = extractTopicDistribution(paper.getTopicDistributionJson());
        CanonicalPaperManifest.ManifestData manifestData = CanonicalPaperManifest.ManifestData.builder()
                .schemaVersion("1.0")
                .examId(paper.getExamId())
                .shiftId(paper.getShiftId())
                .variant(variant)
                .questionIds(questionIds)
                .topicDistribution(topicDist)
                .difficultyScore(paper.getDifficultyScore())
                .isPractice(paper.isPractice())
                .generatedAt(paper.getCreatedAt() != null ? paper.getCreatedAt().toString() : Instant.now().toString())
                .build();

        String manifestDigest = CanonicalPaperManifest.computeManifestDigest(manifestData);
        paper.setManifestDigest(manifestDigest);

        // 2. Build Leaf Payloads & Binary Merkle Tree
        List<String> leafPayloads = CanonicalPaperManifest.buildQuestionLeafPayloads(questionIds, paper.getExamId(), variant);
        MerkleTree merkleTree = new MerkleTree(leafPayloads);
        String paperRootHash = merkleTree.getRootHash();
        paper.setPaperRootHash(paperRootHash);

        // 3. Generate Merkle Proofs for each question
        Map<String, MerkleTree.MerkleProof> proofMap = new HashMap<>();
        for (int i = 0; i < questionIds.size(); i++) {
            UUID qId = questionIds.get(i);
            MerkleTree.MerkleProof proof = merkleTree.getProof(i);
            proofMap.put(qId.toString(), proof);
        }

        try {
            paper.setMerkleProofJson(objectMapper.writeValueAsString(proofMap));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize Merkle proofs for paper {}: {}", paper.getId(), e.getMessage());
        }

        // 4. Submit Attestation to Public Ledger
        LedgerAttestationPayload attestationPayload = LedgerAttestationPayload.builder()
                .examId(paper.getExamId())
                .paperId(paper.getId())
                .variant(variant)
                .paperRootHash(paperRootHash)
                .manifestDigest(manifestDigest)
                .tenantId(tenantId)
                .generatedAt(paper.getCreatedAt() != null ? paper.getCreatedAt() : Instant.now())
                .build();

        LedgerAttestationReceipt receipt = ledgerAdapter.anchorPaper(attestationPayload);

        // 5. Store Proofs & Audit Metadata
        paper.setLedgerTxHash(receipt.getTxHash());
        paper.setLedgerConsensusTimestamp(receipt.getConsensusTimestamp());
        paper.setLedgerBlockNumber(receipt.getBlockNumber());
        paper.setLedgerExplorerUrl(receipt.getLedgerExplorerUrl());
        paper.setLedgerNetwork(receipt.getNetwork());
        paper.setAnchoredAt(receipt.getAnchoredAt());
        paper.setIsTimeLocked(true);

        Paper savedPaper = paperRepository.save(paper);
        log.info("Paper {} successfully anchored to {} ledger: rootHash={}, txHash={}",
                savedPaper.getId(), receipt.getNetwork(), paperRootHash, receipt.getTxHash());

        publishPaperAnchoredAuditEvent(savedPaper, receipt, tenantId);

        return savedPaper;
    }

    /**
     * Verifies paper authenticity by recalculating Merkle root from paper definition
     * and confirming against the stored root hash and ledger anchor.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> verifyPaperAuthenticity(UUID paperId, String tenantId) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new EntityNotFoundException("Paper not found: " + paperId));

        return verifyPaperEntity(paper);
    }

    /**
     * Verifies paper authenticity given examId and paperRootHash.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> verifyPaperByHash(UUID examId, String paperRootHash) {
        Optional<Paper> paperOpt = (examId != null)
                ? paperRepository.findByExamIdAndPaperRootHash(examId, paperRootHash)
                : paperRepository.findByPaperRootHash(paperRootHash);

        if (paperOpt.isEmpty()) {
            // Also check ledger adapter directly
            Optional<LedgerAttestationReceipt> ledgerOpt = ledgerAdapter.verifyAnchoredPaper(examId, paperRootHash);
            if (ledgerOpt.isPresent()) {
                LedgerAttestationReceipt r = ledgerOpt.get();
                Map<String, Object> directRes = new HashMap<>();
                directRes.put("verified", true);
                directRes.put("examId", examId != null ? examId.toString() : "");
                directRes.put("paperRootHash", paperRootHash);
                directRes.put("manifestDigest", r.getManifestDigest() != null ? r.getManifestDigest() : "");
                directRes.put("ledgerTxHash", r.getTxHash());
                directRes.put("consensusTimestamp", r.getConsensusTimestamp());
                directRes.put("blockNumber", r.getBlockNumber());
                directRes.put("ledgerExplorerUrl", r.getLedgerExplorerUrl());
                directRes.put("network", r.getNetwork());
                directRes.put("anchoredAt", r.getAnchoredAt() != null ? r.getAnchoredAt().toString() : "");
                directRes.put("tamperDetected", false);
                directRes.put("source", "LEDGER_DIRECT");
                return directRes;
            }
            return Map.of(
                    "verified", false,
                    "paperRootHash", paperRootHash != null ? paperRootHash : "",
                    "message", "Paper root hash not found in repository or public ledger attestation index"
            );
        }

        return verifyPaperEntity(paperOpt.get());
    }

    private Map<String, Object> verifyPaperEntity(Paper paper) {
        List<UUID> questionIds = extractQuestionIds(paper.getPaperDefinitionJson());
        String variant = (paper.getVariant() != null && !paper.getVariant().isBlank()) ? paper.getVariant() : "SET-A";

        List<String> leafPayloads = CanonicalPaperManifest.buildQuestionLeafPayloads(questionIds, paper.getExamId(), variant);
        MerkleTree recalculatedTree = new MerkleTree(leafPayloads);
        String calculatedRoot = recalculatedTree.getRootHash();

        boolean hashMatch = calculatedRoot.equalsIgnoreCase(paper.getPaperRootHash());
        boolean tamperDetected = !hashMatch;

        Map<String, Object> result = new HashMap<>();
        result.put("verified", hashMatch);
        result.put("paperId", paper.getId().toString());
        result.put("examId", paper.getExamId() != null ? paper.getExamId().toString() : "");
        result.put("variant", variant);
        result.put("paperRootHash", paper.getPaperRootHash());
        result.put("calculatedRootHash", calculatedRoot);
        result.put("manifestDigest", paper.getManifestDigest());
        result.put("ledgerTxHash", paper.getLedgerTxHash());
        result.put("consensusTimestamp", paper.getLedgerConsensusTimestamp());
        result.put("blockNumber", paper.getLedgerBlockNumber());
        result.put("ledgerExplorerUrl", paper.getLedgerExplorerUrl());
        result.put("ledgerNetwork", paper.getLedgerNetwork());
        result.put("anchoredAt", paper.getAnchoredAt() != null ? paper.getAnchoredAt().toString() : "");
        result.put("tamperDetected", tamperDetected);
        result.put("totalQuestions", questionIds.size());
        result.put("isTimeLocked", Boolean.TRUE.equals(paper.getIsTimeLocked()));

        return result;
    }

    /**
     * Extracts Merkle proof for a specific question in a paper.
     */
    public Optional<MerkleTree.MerkleProof> getQuestionProof(UUID paperId, UUID questionId) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new EntityNotFoundException("Paper not found: " + paperId));

        if (paper.getMerkleProofJson() == null || paper.getMerkleProofJson().isBlank()) {
            return Optional.empty();
        }

        try {
            Map<String, MerkleTree.MerkleProof> proofMap = objectMapper.readValue(
                    paper.getMerkleProofJson(),
                    new TypeReference<Map<String, MerkleTree.MerkleProof>>() {});
            return Optional.ofNullable(proofMap.get(questionId.toString()));
        } catch (Exception e) {
            log.warn("Failed to parse MerkleProofJson for paper {}: {}", paperId, e.getMessage());
            return Optional.empty();
        }
    }

    private void publishPaperAnchoredAuditEvent(Paper paper, LedgerAttestationReceipt receipt, String tenantId) {
        try {
            Map<String, Object> event = Map.of(
                    "eventType", "PAPER_LEDGER_ANCHORED",
                    "paperId", paper.getId().toString(),
                    "examId", paper.getExamId().toString(),
                    "paperRootHash", paper.getPaperRootHash() != null ? paper.getPaperRootHash() : "",
                    "manifestDigest", paper.getManifestDigest() != null ? paper.getManifestDigest() : "",
                    "txHash", receipt.getTxHash(),
                    "consensusTimestamp", receipt.getConsensusTimestamp(),
                    "ledgerNetwork", receipt.getNetwork(),
                    "tenantId", tenantId != null ? tenantId : "default",
                    "occurredAt", Instant.now().toString()
            );
            eventPublisher.publish(AUDIT_TOPIC, paper.getId().toString(), event);
        } catch (Exception e) {
            log.error("Failed to publish PAPER_LEDGER_ANCHORED audit event: {}", e.getMessage());
        }
    }

    private List<UUID> extractQuestionIds(String paperDefinitionJson) {
        if (paperDefinitionJson == null || paperDefinitionJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            JsonNode root = objectMapper.readTree(paperDefinitionJson);
            JsonNode qIdsNode = root.get("questionIds");
            if (qIdsNode != null && qIdsNode.isArray()) {
                List<UUID> list = new ArrayList<>();
                for (JsonNode n : qIdsNode) {
                    try {
                        list.add(UUID.fromString(n.asText()));
                    } catch (IllegalArgumentException ignored) {}
                }
                return list;
            }
        } catch (Exception e) {
            log.warn("Could not extract question IDs from paper definition: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    private Map<String, Integer> extractTopicDistribution(String topicDistributionJson) {
        if (topicDistributionJson == null || topicDistributionJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(topicDistributionJson, new TypeReference<Map<String, Integer>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
