package com.examplatform.candidate.service;

import com.examplatform.candidate.domain.CandidateProfile;
import com.examplatform.candidate.exception.ProfileNotFoundException;
import com.examplatform.candidate.repository.CandidateProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for consent recording in {@link CandidateProfileService}.
 *
 * <p><strong>Validates: Requirements 25.3</strong>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CandidateProfileService - Consent Recording")
class ConsentRecordingTest {

    @Mock
    CandidateProfileRepository candidateProfileRepository;

    @Mock
    HashingService hashingService;

    @Mock
    VaultCryptoService vaultCryptoService;

    @InjectMocks
    CandidateProfileService candidateProfileService;

    private static final String TENANT_ID = "exam-authority-1";
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private CandidateProfile profileWithoutConsent() {
        CandidateProfile profile = CandidateProfile.builder()
                .userId(USER_ID)
                .fullName("Test Candidate")
                .mobileHash("hash123")
                .identityDocHash("dochash123")
                .identityDocHmac("dochmac123")
                .consentRecorded(false)
                .consentTimestamp(null)
                .build();
        profile.setTenantId(TENANT_ID);
        return profile;
    }

    @Test
    @DisplayName("records consent — sets consentRecorded=true and consentTimestamp")
    void recordsConsentSetsFieldsCorrectly() {
        CandidateProfile profile = profileWithoutConsent();
        LocalDateTime before = LocalDateTime.now();

        when(candidateProfileRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID))
                .thenReturn(Optional.of(profile));
        when(candidateProfileRepository.save(any(CandidateProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        candidateProfileService.recordConsent(USER_ID, TENANT_ID);

        ArgumentCaptor<CandidateProfile> captor = ArgumentCaptor.forClass(CandidateProfile.class);
        verify(candidateProfileRepository).save(captor.capture());

        CandidateProfile saved = captor.getValue();
        assertThat(saved.isConsentRecorded()).isTrue();
        assertThat(saved.getConsentTimestamp()).isNotNull();
        assertThat(saved.getConsentTimestamp()).isAfterOrEqualTo(before);
        assertThat(saved.getConsentTimestamp()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("throws ProfileNotFoundException when profile does not exist")
    void throwsWhenProfileNotFound() {
        when(candidateProfileRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> candidateProfileService.recordConsent(USER_ID, TENANT_ID))
                .isInstanceOf(ProfileNotFoundException.class)
                .hasMessageContaining("Profile not found");

        verify(candidateProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("consent already recorded — idempotent, overwrites timestamp")
    void idempotentOverwritesTimestamp() {
        CandidateProfile profile = profileWithoutConsent();
        LocalDateTime oldTimestamp = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
        profile.setConsentRecorded(true);
        profile.setConsentTimestamp(oldTimestamp);

        when(candidateProfileRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID))
                .thenReturn(Optional.of(profile));
        when(candidateProfileRepository.save(any(CandidateProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        candidateProfileService.recordConsent(USER_ID, TENANT_ID);

        ArgumentCaptor<CandidateProfile> captor = ArgumentCaptor.forClass(CandidateProfile.class);
        verify(candidateProfileRepository).save(captor.capture());

        CandidateProfile saved = captor.getValue();
        assertThat(saved.isConsentRecorded()).isTrue();
        assertThat(saved.getConsentTimestamp()).isNotNull();
        assertThat(saved.getConsentTimestamp()).isAfter(oldTimestamp);
    }
}
