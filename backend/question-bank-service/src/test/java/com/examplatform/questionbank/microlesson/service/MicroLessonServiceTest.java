// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.service;

import com.examplatform.questionbank.domain.Subject;
import com.examplatform.questionbank.domain.Subtopic;
import com.examplatform.questionbank.domain.Topic;
import com.examplatform.questionbank.microlesson.ai.KaTexValidator;
import com.examplatform.questionbank.microlesson.ai.MicroLessonGeneratorService;
import com.examplatform.questionbank.microlesson.domain.SubtopicMicroLesson;
import com.examplatform.questionbank.microlesson.dto.*;
import com.examplatform.questionbank.microlesson.repository.SubtopicMicroLessonRepository;
import com.examplatform.questionbank.repository.SubjectRepository;
import com.examplatform.questionbank.repository.SubtopicRepository;
import com.examplatform.questionbank.repository.TopicRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MicroLessonService Manual Authoring & Lifecycle Unit Tests")
class MicroLessonServiceTest {

    @Mock
    private SubtopicMicroLessonRepository repository;

    @Mock
    private MicroLessonGeneratorService generatorService;

    @Mock
    private MicroLessonCacheService cacheService;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private SubtopicRepository subtopicRepository;

    private KaTexValidator kaTexValidator;
    private ObjectMapper objectMapper;
    private MicroLessonService service;

    private static final UUID LESSON_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID AUTHOR_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @BeforeEach
    void setUp() {
        kaTexValidator = new KaTexValidator();
        objectMapper = new ObjectMapper();
        service = new MicroLessonService(
                repository,
                generatorService,
                cacheService,
                kaTexValidator,
                objectMapper,
                subtopicRepository,
                topicRepository,
                subjectRepository
        );
    }

    private void stubTaxonomyRepositories() {
        Subject subject = Subject.builder().name("Physics").build();
        ReflectionTestUtils.setField(subject, "id", 1L);
        Topic topic = Topic.builder().subjectId(1L).name("Rotational Mechanics").build();
        ReflectionTestUtils.setField(topic, "id", 10L);
        Subtopic subtopic = Subtopic.builder().topicId(10L).name("Moment of Inertia").build();
        ReflectionTestUtils.setField(subtopic, "id", 100L);

        lenient().when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        lenient().when(topicRepository.findById(10L)).thenReturn(Optional.of(topic));
        lenient().when(subtopicRepository.findById(100L)).thenReturn(Optional.of(subtopic));
    }

    @Nested
    @DisplayName("createManualMicroLesson")
    class CreateManualMicroLessonTests {

        @Test
        @DisplayName("Creates manual micro-lesson with structured content, resolves taxonomy, and defaults status to APPROVED")
        void testCreateManualMicroLessonSuccess() {
            stubTaxonomyRepositories();
            when(repository.findTopBySubtopicIdAndTargetExamOrderByContentVersionDesc(100L, "JEE_MAIN"))
                    .thenReturn(Optional.empty());
            when(repository.save(any(SubtopicMicroLesson.class))).thenAnswer(inv -> {
                SubtopicMicroLesson saved = inv.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", LESSON_ID);
                return saved;
            });

            CreateMicroLessonRequest request = CreateMicroLessonRequest.builder()
                    .subjectId(1L)
                    .topicId(10L)
                    .subtopicId(100L)
                    .targetExam("JEE_MAIN")
                    .coreConcept("Moment of inertia $$I = \\sum m_i r_i^2$$ measures resistance to angular acceleration.")
                    .trapAlert(new TrapAlertDto(
                            "Axis Selection Pitfall",
                            "Always verify whether the axis passes through the center of mass.",
                            "Using $$I = \\frac{1}{2}MR^2$$ for a tangent axis without parallel axis theorem.",
                            "Apply $$I = I_{cm} + Md^2$$ whenever axis is shifted by distance $$d$$."
                    ))
                    .formulaCheatSheet(List.of(
                            new FormulaEntryDto(
                                    "Parallel Axis Theorem",
                                    "$$I = I_{cm} + Md^2$$",
                                    "Moment of inertia about any parallel axis at distance $$d$$",
                                    List.of("$$I_{cm}$$: COM inertia", "$$M$$: Total mass", "$$d$$: Perpendicular shift")
                            )
                    ))
                    .stepByStepBreakdown(new StepByStepBreakdownDto(
                            "Find the moment of inertia of a solid disc of mass $$M$$ and radius $$R$$ about a tangent in its plane.",
                            null,
                            "$$\\frac{5}{4}MR^2$$",
                            List.of(
                                    new StepItemDto(1, "Diameter Axis", "By perpendicular axis theorem, $$I_d = \\frac{1}{4}MR^2$$.", "$$I_d = \\frac{1}{4}MR^2$$", null),
                                    new StepItemDto(2, "Parallel Shift", "Shift by $$d = R$$ in the plane: $$I = \\frac{1}{4}MR^2 + MR^2 = \\frac{5}{4}MR^2$$.", "$$I = \\frac{5}{4}MR^2$$", "Final Answer: $$\\frac{5}{4}MR^2$$")
                            ),
                            null, null, null, null
                    ))
                    .keyTakeaways(List.of(
                            "Moment of inertia depends on the choice of axis.",
                            "Perpendicular axis theorem applies strictly to planar laminas."
                    ))
                    .quickCheckItems(List.of(
                            new QuickCheckItemDto(
                                    "What is the moment of inertia of a thin ring of mass $$M$$ and radius $$R$$ about its central axis?",
                                    List.of("$$MR^2$$", "$$\\frac{1}{2}MR^2$$", "$$2MR^2$$", "$$\\frac{2}{5}MR^2$$"),
                                    "$$MR^2$$",
                                    "All mass elements are at distance $$R$$ from the axis."
                            )
                    ))
                    .build();

            MicroLessonResponse response = service.createManualMicroLesson(request, AUTHOR_ID);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(LESSON_ID);
            assertThat(response.subject()).isEqualTo("Physics");
            assertThat(response.topic()).isEqualTo("Rotational Mechanics");
            assertThat(response.subtopic()).isEqualTo("Moment of Inertia");
            assertThat(response.status()).isEqualTo("APPROVED");
            assertThat(response.contentVersion()).isEqualTo(1);
            assertThat(response.reviewedBy()).isEqualTo(AUTHOR_ID);
            assertThat(response.getContent().trapAlert().title()).isEqualTo("Axis Selection Pitfall");
            assertThat(response.getContent().formulaCheatSheet()).hasSize(1);
            assertThat(response.getContent().formulaCheatSheet().get(0).name()).isEqualTo("Parallel Axis Theorem");
            assertThat(response.getContent().stepByStepBreakdown()).hasSize(2);
            assertThat(response.getContent().keyTakeaways()).hasSize(2);
            assertThat(response.getContent().quickCheckItems()).hasSize(1);

            verify(cacheService).evict(100L);
        }

        @Test
        @DisplayName("Populates Redis cache when created with PUBLISHED status and increments version if previous exists")
        void testCreatePublishedPopulatesCacheAndIncrementsVersion() {
            stubTaxonomyRepositories();
            SubtopicMicroLesson prev = SubtopicMicroLesson.builder().contentVersion(2).build();
            when(repository.findTopBySubtopicIdAndTargetExamOrderByContentVersionDesc(100L, "JEE_MAIN"))
                    .thenReturn(Optional.of(prev));
            when(repository.save(any(SubtopicMicroLesson.class))).thenAnswer(inv -> inv.getArgument(0));

            CreateMicroLessonRequest request = CreateMicroLessonRequest.builder()
                    .subjectId(1L)
                    .topicId(10L)
                    .subtopicId(100L)
                    .targetExam("JEE_MAIN")
                    .coreConcept("Core concept with valid math $$E = mc^2$$.")
                    .trap("Common trap in relativistic energy.")
                    .cheatSheet(List.of("$$E = \\gamma m c^2$$"))
                    .stepByStepBreakdown(new StepByStepBreakdownDto("Find rest energy", List.of("Substitute $$m$$ and $$c$$"), "$$9 \\times 10^{16}\\text{ J}$$"))
                    .status("PUBLISHED")
                    .build();

            MicroLessonResponse response = service.createManualMicroLesson(request, AUTHOR_ID);

            assertThat(response.status()).isEqualTo("PUBLISHED");
            assertThat(response.contentVersion()).isEqualTo(3);
            verify(cacheService).put(eq(100L), any(MicroLessonResponse.class));
        }

        @Test
        @DisplayName("Throws 400 BAD_REQUEST when taxonomy ID is not found in repository")
        void testCreateFailsWhenTaxonomyNotFound() {
            when(subjectRepository.findById(999L)).thenReturn(Optional.empty());

            CreateMicroLessonRequest request = CreateMicroLessonRequest.builder()
                    .subjectId(999L)
                    .topicId(10L)
                    .subtopicId(100L)
                    .targetExam("JEE_MAIN")
                    .coreConcept("Valid concept")
                    .trap("Valid trap")
                    .cheatSheet(List.of("$$a = b$$"))
                    .stepByStepBreakdown(new StepByStepBreakdownDto("Stem", List.of("Step 1"), "Ans"))
                    .build();

            assertThatThrownBy(() -> service.createManualMicroLesson(request, AUTHOR_ID))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(rse.getReason()).contains("Subject not found");
                    });
        }

        @Test
        @DisplayName("Throws 400 BAD_REQUEST when KaTeX validation fails on malformed LaTeX")
        void testCreateFailsOnMalformedLatex() {
            stubTaxonomyRepositories();

            CreateMicroLessonRequest request = CreateMicroLessonRequest.builder()
                    .subjectId(1L)
                    .topicId(10L)
                    .subtopicId(100L)
                    .targetExam("JEE_MAIN")
                    .coreConcept("Broken formula $$\\frac{a}{b$$ with unclosed brace and delimiter.")
                    .trap("Valid trap")
                    .cheatSheet(List.of("$$I = \\frac{1}{2}MR^2$$"))
                    .stepByStepBreakdown(new StepByStepBreakdownDto("Stem", List.of("Step 1"), "Ans"))
                    .build();

            assertThatThrownBy(() -> service.createManualMicroLesson(request, AUTHOR_ID))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(rse.getReason()).contains("KaTeX validation failed");
                    });

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateMicroLesson")
    class UpdateMicroLessonTests {

        @Test
        @DisplayName("Updates existing micro-lesson, validates LaTeX, and increments contentVersion")
        void testUpdateMicroLessonSuccess() {
            SubtopicMicroLesson existing = SubtopicMicroLesson.builder()
                    .subjectId(1L)
                    .topicId(10L)
                    .subtopicId(100L)
                    .subject("Physics")
                    .topic("Rotational Mechanics")
                    .subtopic("Moment of Inertia")
                    .targetExam("JEE_MAIN")
                    .coreConcept("Old core concept")
                    .trap("Old trap")
                    .cheatSheetJson("[\"$$I = mr^2$$\"]")
                    .stepByStepBreakdownJson("{\"questionStem\":\"Old stem\",\"steps\":[\"Old step\"],\"finalAnswer\":\"Old ans\"}")
                    .status("IN_REVIEW")
                    .contentVersion(1)
                    .build();
            ReflectionTestUtils.setField(existing, "id", LESSON_ID);

            when(repository.findById(LESSON_ID)).thenReturn(Optional.of(existing));
            when(repository.findTopBySubtopicIdAndTargetExamOrderByContentVersionDesc(100L, "JEE_ADVANCED"))
                    .thenReturn(Optional.empty());
            when(repository.save(any(SubtopicMicroLesson.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateMicroLessonRequest updateReq = UpdateMicroLessonRequest.builder()
                    .targetExam("JEE_ADVANCED")
                    .coreConcept("Updated core concept with $$L = I \\omega$$.")
                    .formulaCheatSheet(List.of(
                            new FormulaEntryDto("Angular Momentum", "$$L = I \\omega$$", "Conservation law", List.of())
                    ))
                    .keyTakeaways(List.of("Updated takeaway 1"))
                    .status("APPROVED")
                    .reviewComments("Updated in Authoring Studio")
                    .build();

            MicroLessonResponse response = service.updateMicroLesson(LESSON_ID, updateReq, AUTHOR_ID);

            assertThat(response.targetExam()).isEqualTo("JEE_ADVANCED");
            assertThat(response.coreConcept()).isEqualTo("Updated core concept with $$L = I \\omega$$.");
            assertThat(response.contentVersion()).isEqualTo(2);
            assertThat(response.status()).isEqualTo("APPROVED");
            assertThat(response.reviewComments()).isEqualTo("Updated in Authoring Studio");
            assertThat(response.getContent().formulaCheatSheet()).hasSize(1);
            assertThat(response.getContent().keyTakeaways()).containsExactly("Updated takeaway 1");
        }

        @Test
        @DisplayName("Throws 400 BAD_REQUEST when updated content contains invalid LaTeX")
        void testUpdateMicroLessonInvalidLatex() {
            SubtopicMicroLesson existing = SubtopicMicroLesson.builder()
                    .subjectId(1L)
                    .topicId(10L)
                    .subtopicId(100L)
                    .subject("Physics")
                    .topic("Rotational Mechanics")
                    .subtopic("Moment of Inertia")
                    .targetExam("JEE_MAIN")
                    .coreConcept("Valid concept")
                    .trap("Valid trap")
                    .cheatSheetJson("[\"$$I = mr^2$$\"]")
                    .stepByStepBreakdownJson("{\"questionStem\":\"Stem\",\"steps\":[\"Step 1\"],\"finalAnswer\":\"Ans\"}")
                    .status("APPROVED")
                    .contentVersion(1)
                    .build();

            when(repository.findById(LESSON_ID)).thenReturn(Optional.of(existing));

            UpdateMicroLessonRequest updateReq = UpdateMicroLessonRequest.builder()
                    .formulaCheatSheet(List.of(
                            new FormulaEntryDto("Broken Formula", "$$\\frac{1}{2", "Unclosed brace and $$", List.of())
                    ))
                    .build();

            assertThatThrownBy(() -> service.updateMicroLesson(LESSON_ID, updateReq, AUTHOR_ID))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(rse.getReason()).contains("KaTeX validation failed");
                    });

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Throws 404 NOT_FOUND when updating non-existent micro-lesson")
        void testUpdateMicroLessonNotFound() {
            when(repository.findById(LESSON_ID)).thenReturn(Optional.empty());

            UpdateMicroLessonRequest updateReq = UpdateMicroLessonRequest.builder()
                    .coreConcept("Updated concept")
                    .build();

            assertThatThrownBy(() -> service.updateMicroLesson(LESSON_ID, updateReq, AUTHOR_ID))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("getMicroLessonById")
    class GetMicroLessonByIdTests {

        @Test
        @DisplayName("Returns MicroLessonResponse when ID exists")
        void testGetByIdSuccess() {
            SubtopicMicroLesson existing = SubtopicMicroLesson.builder()
                    .subjectId(1L)
                    .topicId(10L)
                    .subtopicId(100L)
                    .subject("Physics")
                    .topic("Rotational Mechanics")
                    .subtopic("Moment of Inertia")
                    .targetExam("JEE_MAIN")
                    .coreConcept("Concept")
                    .trap("Trap")
                    .cheatSheetJson("[\"$$I = mr^2$$\"]")
                    .stepByStepBreakdownJson("{\"questionStem\":\"Stem\",\"steps\":[\"Step 1\"],\"finalAnswer\":\"Ans\"}")
                    .status("APPROVED")
                    .contentVersion(1)
                    .build();
            ReflectionTestUtils.setField(existing, "id", LESSON_ID);

            when(repository.findById(LESSON_ID)).thenReturn(Optional.of(existing));

            MicroLessonResponse response = service.getMicroLessonById(LESSON_ID);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(LESSON_ID);
            assertThat(response.subtopic()).isEqualTo("Moment of Inertia");
        }

        @Test
        @DisplayName("Throws 404 NOT_FOUND when ID does not exist")
        void testGetByIdNotFound() {
            when(repository.findById(LESSON_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getMicroLessonById(LESSON_ID))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        }
    }
}
