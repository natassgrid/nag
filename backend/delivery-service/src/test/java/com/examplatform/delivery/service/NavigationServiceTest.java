package com.examplatform.delivery.service;

import com.examplatform.delivery.domain.ExamSession;
import com.examplatform.delivery.domain.ExamSession.ExamSessionStatus;
import com.examplatform.delivery.dto.NavigationRequest;
import com.examplatform.delivery.dto.NavigationResponse;
import com.examplatform.delivery.dto.NavigationResponse.NavigationAction;
import com.examplatform.delivery.dto.NavigationResponse.NavigationPolicy;
import com.examplatform.delivery.exception.NavigationPolicyViolationException;
import com.examplatform.delivery.repository.ExamSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NavigationService")
class NavigationServiceTest {

    @Mock
    private ExamSessionRepository examSessionRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private NavigationService navigationService;

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID CANDIDATE_ID = UUID.randomUUID();
    private static final UUID EXAM_ID = UUID.randomUUID();
    private static final UUID SHIFT_ID = UUID.randomUUID();
    private static final UUID PAPER_ID = UUID.randomUUID();
    private static final String TENANT_ID = "tenant-001";

    private ExamSession activeSession;

    @BeforeEach
    void setUp() {
        navigationService = new NavigationService(examSessionRepository, redisTemplate);

        activeSession = ExamSession.builder()
                .sessionId(SESSION_ID)
                .candidateId(CANDIDATE_ID)
                .examId(EXAM_ID)
                .shiftId(SHIFT_ID)
                .paperId(PAPER_ID)
                .status(ExamSessionStatus.ACTIVE)
                .currentQuestionIndex(5)
                .languageCode("en")
                .fullScreenExitCount(0)
                .startedAt(Instant.now())
                .scheduledEndAt(Instant.now().plusSeconds(10800))
                .build();
    }

    @Nested
    @DisplayName("Sequential Policy")
    class SequentialPolicyTests {

        @Test
        @DisplayName("Sequential policy allows NEXT navigation (+1)")
        void sequential_allowsNext() {
            // Given - validate directly
            // When / Then — should not throw
            navigationService.validateNavigation(
                    NavigationPolicy.SEQUENTIAL, 5, 6, null, activeSession);
        }

        @Test
        @DisplayName("Sequential policy blocks PREV navigation")
        void sequential_blocksPrev() {
            // When / Then
            assertThatThrownBy(() ->
                    navigationService.validateNavigation(
                            NavigationPolicy.SEQUENTIAL, 5, 4, null, activeSession))
                    .isInstanceOf(NavigationPolicyViolationException.class)
                    .hasMessageContaining("SEQUENTIAL")
                    .hasMessageContaining("backward navigation");
        }

        @Test
        @DisplayName("Sequential policy blocks JUMP navigation")
        void sequential_blocksJump() {
            // When / Then
            assertThatThrownBy(() ->
                    navigationService.validateNavigation(
                            NavigationPolicy.SEQUENTIAL, 5, 10, null, activeSession))
                    .isInstanceOf(NavigationPolicyViolationException.class)
                    .hasMessageContaining("SEQUENTIAL")
                    .hasMessageContaining("jumping ahead");
        }

        @Test
        @DisplayName("Sequential policy computes only NEXT as allowed action")
        void sequential_computesOnlyNext() {
            // When
            List<NavigationAction> actions = navigationService.computeAllowedActions(
                    NavigationPolicy.SEQUENTIAL, 5, activeSession);

            // Then
            assertThat(actions).containsExactly(NavigationAction.NEXT);
        }

        @Test
        @DisplayName("Sequential policy at last question has no allowed actions")
        void sequential_atLastQuestion_noActions() {
            // Given — totalQuestions defaults to 50, so index 49 is last
            activeSession.setCurrentQuestionIndex(49);

            // When
            List<NavigationAction> actions = navigationService.computeAllowedActions(
                    NavigationPolicy.SEQUENTIAL, 49, activeSession);

            // Then
            assertThat(actions).isEmpty();
        }
    }

    @Nested
    @DisplayName("Flexible Policy")
    class FlexiblePolicyTests {

        @Test
        @DisplayName("Flexible policy allows NEXT navigation")
        void flexible_allowsNext() {
            // When / Then — no exception
            navigationService.validateNavigation(
                    NavigationPolicy.FLEXIBLE, 5, 6, null, activeSession);
        }

        @Test
        @DisplayName("Flexible policy allows PREV navigation")
        void flexible_allowsPrev() {
            // When / Then — no exception
            navigationService.validateNavigation(
                    NavigationPolicy.FLEXIBLE, 5, 4, null, activeSession);
        }

        @Test
        @DisplayName("Flexible policy allows JUMP navigation")
        void flexible_allowsJump() {
            // When / Then — no exception
            navigationService.validateNavigation(
                    NavigationPolicy.FLEXIBLE, 5, 30, null, activeSession);
        }

        @Test
        @DisplayName("Flexible policy computes NEXT, PREV, JUMP, SECTION_SWITCH")
        void flexible_computesAllActions() {
            // When
            List<NavigationAction> actions = navigationService.computeAllowedActions(
                    NavigationPolicy.FLEXIBLE, 5, activeSession);

            // Then
            assertThat(actions).containsExactlyInAnyOrder(
                    NavigationAction.NEXT,
                    NavigationAction.PREV,
                    NavigationAction.JUMP,
                    NavigationAction.SECTION_SWITCH
            );
        }

        @Test
        @DisplayName("Flexible policy at index 0 omits PREV")
        void flexible_atFirstQuestion_noPrev() {
            // When
            List<NavigationAction> actions = navigationService.computeAllowedActions(
                    NavigationPolicy.FLEXIBLE, 0, activeSession);

            // Then
            assertThat(actions).contains(NavigationAction.NEXT, NavigationAction.JUMP, NavigationAction.SECTION_SWITCH);
            assertThat(actions).doesNotContain(NavigationAction.PREV);
        }
    }

    @Nested
    @DisplayName("Restricted Policy")
    class RestrictedPolicyTests {

        @Test
        @DisplayName("Restricted policy allows NEXT within same section")
        void restricted_allowsNextWithinSection() {
            // Given — questions 5 and 6 are in the same section (0-9 = section 0)
            // When / Then — no exception
            navigationService.validateNavigation(
                    NavigationPolicy.RESTRICTED, 5, 6, null, activeSession);
        }

        @Test
        @DisplayName("Restricted policy allows PREV within same section")
        void restricted_allowsPrevWithinSection() {
            // Given — questions 5 and 4 are in the same section
            // When / Then — no exception
            navigationService.validateNavigation(
                    NavigationPolicy.RESTRICTED, 5, 4, null, activeSession);
        }

        @Test
        @DisplayName("Restricted policy blocks cross-section JUMP")
        void restricted_blocksCrossSectionJump() {
            // Given — question 5 is section 0, question 15 is section 1
            // When / Then
            assertThatThrownBy(() ->
                    navigationService.validateNavigation(
                            NavigationPolicy.RESTRICTED, 5, 15, null, activeSession))
                    .isInstanceOf(NavigationPolicyViolationException.class)
                    .hasMessageContaining("RESTRICTED")
                    .hasMessageContaining("cross-section");
        }

        @Test
        @DisplayName("Restricted policy blocks jump within section (non-adjacent)")
        void restricted_blocksJumpWithinSection() {
            // Given — questions 2 and 7 are same section but distance > 1
            // When / Then
            assertThatThrownBy(() ->
                    navigationService.validateNavigation(
                            NavigationPolicy.RESTRICTED, 2, 7, null, activeSession))
                    .isInstanceOf(NavigationPolicyViolationException.class)
                    .hasMessageContaining("RESTRICTED")
                    .hasMessageContaining("jumping within section");
        }

        @Test
        @DisplayName("Restricted policy blocks navigation from section boundary to next section")
        void restricted_blocksBoundaryToNextSection() {
            // Given — question 9 is section 0, question 10 is section 1
            // When / Then
            assertThatThrownBy(() ->
                    navigationService.validateNavigation(
                            NavigationPolicy.RESTRICTED, 9, 10, null, activeSession))
                    .isInstanceOf(NavigationPolicyViolationException.class)
                    .hasMessageContaining("RESTRICTED")
                    .hasMessageContaining("cross-section");
        }
    }

    @Nested
    @DisplayName("Full Navigate Flow")
    class FullNavigateFlowTests {

        @Test
        @DisplayName("Navigate updates session index and returns response")
        void navigate_updatesSessionAndReturnsResponse() {
            // Given
            NavigationRequest request = NavigationRequest.builder()
                    .sessionId(SESSION_ID)
                    .targetQuestionIndex(6)
                    .build();

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("session:" + SESSION_ID)).thenReturn(activeSession);
            when(examSessionRepository.save(any(ExamSession.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            NavigationResponse response = navigationService.navigate(request, CANDIDATE_ID, TENANT_ID);

            // Then
            assertThat(response.getSessionId()).isEqualTo(SESSION_ID);
            assertThat(response.getCurrentQuestionIndex()).isEqualTo(6);
            assertThat(response.getNavigationPolicy()).isEqualTo(NavigationPolicy.FLEXIBLE);
            assertThat(response.getAllowedActions()).isNotEmpty();
        }

        @Test
        @DisplayName("Navigate rejects out-of-bounds target index")
        void navigate_rejectsOutOfBounds() {
            // Given
            NavigationRequest request = NavigationRequest.builder()
                    .sessionId(SESSION_ID)
                    .targetQuestionIndex(-1)
                    .build();

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("session:" + SESSION_ID)).thenReturn(activeSession);

            // When / Then
            assertThatThrownBy(() -> navigationService.navigate(request, CANDIDATE_ID, TENANT_ID))
                    .isInstanceOf(NavigationPolicyViolationException.class)
                    .hasMessageContaining("out of bounds");
        }
    }
}
