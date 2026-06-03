package com.examplatform.translation.repository;

import com.examplatform.translation.domain.Translation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for translations.
 */
@Repository
public interface TranslationRepository extends JpaRepository<Translation, UUID> {

    List<Translation> findByQuestionIdAndTenantId(UUID questionId, String tenantId);

    List<Translation> findByQuestionIdAndLanguageCodeAndTenantId(UUID questionId, String languageCode, String tenantId);

    List<Translation> findByQuestionIdAndStatusAndTenantId(UUID questionId, Translation.TranslationStatus status, String tenantId);

    List<Translation> findByTranslatorIdAndTenantId(UUID translatorId, String tenantId);

    List<Translation> findByStatusAndTenantId(Translation.TranslationStatus status, String tenantId);
}
