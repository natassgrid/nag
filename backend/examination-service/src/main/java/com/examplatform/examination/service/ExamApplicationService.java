/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

package com.examplatform.examination.service;

import com.examplatform.examination.domain.ExamApplication;
import com.examplatform.examination.domain.ExamShift;
import com.examplatform.examination.domain.Examination;
import com.examplatform.examination.domain.ExaminationCentre;
import com.examplatform.examination.domain.ExaminationSchedule;
import com.examplatform.examination.dto.AdmitCardResponse;
import com.examplatform.examination.dto.ExamApplicationRequest;
import com.examplatform.examination.dto.ExamApplicationResponse;
import com.examplatform.examination.dto.PublicCentreResponse;
import com.examplatform.examination.repository.ExamApplicationRepository;
import com.examplatform.examination.repository.ExamShiftRepository;
import com.examplatform.examination.repository.ExaminationCentreRepository;
import com.examplatform.examination.repository.ExaminationRepository;
import com.examplatform.examination.repository.ExaminationScheduleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handles candidate exam applications: apply, check eligibility, list applied exams,
 * hall ticket generation, and admit card issuance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamApplicationService {

    private final ExamApplicationRepository applicationRepository;
    private final ExaminationRepository examinationRepository;
    private final ExaminationCentreRepository centreRepository;
    private final ExaminationScheduleRepository scheduleRepository;
    private final ExamShiftRepository shiftRepository;

    private static final List<String> STANDARD_INSTRUCTIONS = List.of(
            "1. Candidates must report at the examination centre strictly at the designated Reporting Time. No candidate will be permitted entry after the Gate Closing Time.",
            "2. Bring a printed copy of this Admit Card along with at least one original valid government photo identity card (Aadhaar Card / PAN Card / Voter ID / Passport / Driving License).",
            "3. Electronic items, mobile phones, Bluetooth devices, smart watches, bags, and unauthorized stationary are strictly prohibited inside the exam hall.",
            "4. Biometric registration, digital photograph capture, and IRIS/fingerprint verification will be conducted at the venue prior to system login.",
            "5. The Computer-Based Test (CBT) will start automatically upon scheduled login. Ensure your designated node and mouse are operating correctly before test initiation.",
            "6. Rough sheets and pens will be provided in the test lab and must be returned to the invigilator before leaving."
    );

    /**
     * Apply candidate with default/empty request payload (backward compatibility).
     */
    @Transactional
    public ExamApplicationResponse apply(UUID examId, UUID candidateId, String tenantId) {
        return apply(examId, candidateId, tenantId, new ExamApplicationRequest());
    }

    /**
     * Apply a candidate for an examination with centre and shift preferences.
     * Auto-allocates centre/shift and generates unique Hall Ticket number and verification QR hash.
     */
    @Transactional
    public ExamApplicationResponse apply(UUID examId, UUID candidateId, String tenantId, ExamApplicationRequest request) {
        Examination exam = examinationRepository.findById(examId)
                .orElseThrow(() -> new EntityNotFoundException("Examination not found: " + examId));

        if (!"PUBLISHED".equalsIgnoreCase(exam.getStatus()) && !"ACTIVE".equalsIgnoreCase(exam.getStatus())) {
            throw new IllegalStateException("Exam is not open for applications. Status: " + exam.getStatus());
        }

        if (applicationRepository.existsByCandidateIdAndExaminationIdAndTenantId(candidateId, examId, tenantId)) {
            throw new DuplicateKeyException("Candidate " + candidateId + " has already applied for exam " + examId);
        }

        // Determine allocated centre
        UUID allocatedCentreId = resolveAllocatedCentre(request, tenantId);

        // Determine allocated shift & schedule
        Optional<ExaminationSchedule> latestScheduleOpt = scheduleRepository
                .findFirstByExaminationIdAndStatusAndTenantIdOrderByScheduleVersionDesc(examId, "PUBLISHED", tenantId);
        if (latestScheduleOpt.isEmpty()) {
            List<ExaminationSchedule> schedules = scheduleRepository
                    .findByExaminationIdAndTenantIdOrderByScheduleVersionDesc(examId, tenantId);
            if (!schedules.isEmpty()) {
                latestScheduleOpt = Optional.of(schedules.get(0));
            }
        }

        UUID allocatedShiftId = request != null ? request.getPreferredShiftId() : null;
        if (allocatedShiftId == null && latestScheduleOpt.isPresent()) {
            List<ExamShift> shifts = shiftRepository.findByScheduleIdOrderByShiftNumber(latestScheduleOpt.get().getId());
            if (!shifts.isEmpty()) {
                allocatedShiftId = shifts.get(0).getId();
            }
        }

        // Generate Hall Ticket Number
        String hallTicketNumber = generateHallTicketNumber(exam.getCode(), candidateId);

        // Generate verification hash
        String verificationHash = generateQrVerificationHash(hallTicketNumber, candidateId, examId, allocatedCentreId);

        ExamApplication application = ExamApplication.builder()
                .candidateId(candidateId)
                .examinationId(examId)
                .tenantId(tenantId)
                .status("CONFIRMED")
                .hallTicketNumber(hallTicketNumber)
                .firstChoiceCentreId(request != null ? request.getFirstChoiceCentreId() : null)
                .secondChoiceCentreId(request != null ? request.getSecondChoiceCentreId() : null)
                .thirdChoiceCentreId(request != null ? request.getThirdChoiceCentreId() : null)
                .preferredShiftId(request != null ? request.getPreferredShiftId() : null)
                .allocatedCentreId(allocatedCentreId)
                .allocatedShiftId(allocatedShiftId)
                .pwdRequired(request != null && Boolean.TRUE.equals(request.getPwdRequired()))
                .scribeRequired(request != null && Boolean.TRUE.equals(request.getScribeRequired()))
                .qrVerificationHash(verificationHash)
                .build();

        application = applicationRepository.save(application);

        log.info("Candidate {} successfully applied for exam {} with Hall Ticket {}",
                candidateId, examId, hallTicketNumber);

        return toResponse(application, exam, allocatedCentreId, latestScheduleOpt.orElse(null), allocatedShiftId);
    }

    /**
     * List all exams the candidate has applied for.
     */
    @Transactional(readOnly = true)
    public List<ExamApplicationResponse> getMyApplications(UUID candidateId, String tenantId) {
        return applicationRepository.findByCandidateIdAndTenantId(candidateId, tenantId)
                .stream()
                .map(app -> {
                    Examination exam = examinationRepository.findById(app.getExaminationId()).orElse(null);
                    ExaminationSchedule schedule = null;
                    if (exam != null) {
                        schedule = scheduleRepository
                                .findFirstByExaminationIdAndStatusAndTenantIdOrderByScheduleVersionDesc(exam.getId(), "PUBLISHED", tenantId)
                                .orElse(null);
                    }
                    return toResponse(app, exam, app.getAllocatedCentreId(), schedule, app.getAllocatedShiftId());
                })
                .collect(Collectors.toList());
    }

    /**
     * Retrieve application details for the authenticated candidate for a specific examination.
     */
    @Transactional(readOnly = true)
    public ExamApplicationResponse getMyApplication(UUID examId, UUID candidateId, String tenantId) {
        ExamApplication app = applicationRepository
                .findByCandidateIdAndExaminationIdAndTenantId(candidateId, examId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("No application found for candidate " + candidateId + " and exam " + examId));

        Examination exam = examinationRepository.findById(examId).orElse(null);
        ExaminationSchedule schedule = null;
        if (exam != null) {
            schedule = scheduleRepository
                    .findFirstByExaminationIdAndStatusAndTenantIdOrderByScheduleVersionDesc(exam.getId(), "PUBLISHED", tenantId)
                    .orElse(null);
        }
        return toResponse(app, exam, app.getAllocatedCentreId(), schedule, app.getAllocatedShiftId());
    }

    /**
     * Retrieve full admit card / hall ticket by Examination ID and authenticated candidate.
     */
    @Transactional(readOnly = true)
    public AdmitCardResponse getAdmitCard(UUID examId, UUID candidateId, String tenantId) {
        ExamApplication app = applicationRepository
                .findByCandidateIdAndExaminationIdAndTenantId(candidateId, examId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("No application found for exam: " + examId));

        return buildAdmitCard(app, tenantId);
    }

    /**
     * Retrieve full admit card / hall ticket by Application ID and authenticated candidate.
     */
    @Transactional(readOnly = true)
    public AdmitCardResponse getAdmitCardByApplicationId(UUID applicationId, UUID candidateId, String tenantId) {
        ExamApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Application not found: " + applicationId));

        if (!app.getCandidateId().equals(candidateId) && !"SUPER_ADMIN".equals(tenantId)) {
            throw new SecurityException("Unauthorized access to hall ticket for application: " + applicationId);
        }

        return buildAdmitCard(app, tenantId);
    }

    /**
     * List active examination centres available across India for candidate preference selection.
     */
    @Transactional(readOnly = true)
    public List<PublicCentreResponse> listPublicCentres(String tenantId, String state, String city) {
        List<ExaminationCentre> centres;
        if (city != null && !city.isBlank()) {
            centres = centreRepository.findByTenantIdAndCityIgnoreCaseAndActiveTrue(tenantId, city.trim());
        } else if (state != null && !state.isBlank()) {
            centres = centreRepository.findByTenantIdAndStateIgnoreCaseAndActiveTrue(tenantId, state.trim());
        } else {
            centres = centreRepository.findByTenantIdAndActiveTrue(tenantId);
        }

        return centres.stream()
                .map(c -> PublicCentreResponse.builder()
                        .id(c.getId())
                        .centreName(c.getCentreName())
                        .region(c.getRegion())
                        .state(c.getState())
                        .district(c.getDistrict())
                        .city(c.getCity())
                        .building(c.getBuilding())
                        .totalCapacity(c.getTotalCapacity())
                        .build())
                .collect(Collectors.toList());
    }

    // ── Helper Methods ──────────────────────────────────────────────────────────

    private UUID resolveAllocatedCentre(ExamApplicationRequest request, String tenantId) {
        if (request != null) {
            if (request.getFirstChoiceCentreId() != null && centreRepository.existsById(request.getFirstChoiceCentreId())) {
                return request.getFirstChoiceCentreId();
            }
            if (request.getSecondChoiceCentreId() != null && centreRepository.existsById(request.getSecondChoiceCentreId())) {
                return request.getSecondChoiceCentreId();
            }
            if (request.getThirdChoiceCentreId() != null && centreRepository.existsById(request.getThirdChoiceCentreId())) {
                return request.getThirdChoiceCentreId();
            }
        }
        List<ExaminationCentre> centres = centreRepository.findByTenantIdAndActiveTrue(tenantId);
        return centres.isEmpty() ? null : centres.get(0).getId();
    }

    private String generateHallTicketNumber(String examCode, UUID candidateId) {
        String prefix = examCode != null && !examCode.isBlank()
                ? examCode.replaceAll("[^A-Za-z0-9]", "").toUpperCase()
                : "NAG2026";
        if (prefix.length() > 8) {
            prefix = prefix.substring(0, 8);
        }
        int randomSuffix = 100000 + new Random().nextInt(900000);
        return String.format("HT-%s-%d", prefix, randomSuffix);
    }

    private String generateQrVerificationHash(String hallTicketNumber, UUID candidateId, UUID examId, UUID centreId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String raw = String.format("%s:%s:%s:%s", hallTicketNumber, candidateId, examId, centreId);
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }

    private AdmitCardResponse buildAdmitCard(ExamApplication app, String tenantId) {
        Examination exam = examinationRepository.findById(app.getExaminationId())
                .orElseThrow(() -> new EntityNotFoundException("Exam not found: " + app.getExaminationId()));

        ExaminationCentre centre = app.getAllocatedCentreId() != null
                ? centreRepository.findById(app.getAllocatedCentreId()).orElse(null)
                : null;

        ExamShift shift = app.getAllocatedShiftId() != null
                ? shiftRepository.findById(app.getAllocatedShiftId()).orElse(null)
                : null;

        ExaminationSchedule schedule = null;
        if (shift != null && shift.getScheduleId() != null) {
            schedule = scheduleRepository.findById(shift.getScheduleId()).orElse(null);
        }
        if (schedule == null) {
            schedule = scheduleRepository
                    .findFirstByExaminationIdAndStatusAndTenantIdOrderByScheduleVersionDesc(exam.getId(), "PUBLISHED", tenantId)
                    .orElse(null);
        }

        LocalDate examDate = schedule != null ? schedule.getExamDate() : LocalDate.now().plusDays(14);
        LocalTime reportingTime = shift != null ? shift.getReportingTime() : LocalTime.of(8, 0);
        LocalTime gateClosingTime = shift != null ? shift.getGateClosingTime() : LocalTime.of(8, 45);
        LocalTime loginStartTime = shift != null ? shift.getLoginStartTime() : LocalTime.of(8, 50);
        LocalTime examStartTime = shift != null ? shift.getExamStartTime() : LocalTime.of(9, 0);
        LocalTime examEndTime = shift != null ? shift.getExamEndTime() : LocalTime.of(10, 0);
        String shiftName = shift != null ? shift.getShiftName() : "Morning Shift (09:00 AM - 10:00 AM)";
        Integer shiftNumber = shift != null ? shift.getShiftNumber() : 1;

        String centreName = centre != null ? centre.getCentreName() : "National Assessment Centre - Sector 62";
        String building = centre != null ? centre.getBuilding() : "Main Academic Building, Wing A";
        String floor = centre != null ? centre.getFloor() : "1st Floor";
        String city = centre != null ? centre.getCity() : "Noida";
        String state = centre != null ? centre.getState() : "Uttar Pradesh";
        String labIdentifier = centre != null && centre.getLaboratoryIdentifier() != null
                ? centre.getLaboratoryIdentifier()
                : "LAB-CBT-01";

        String qrPayload = String.format(
                "{\"ht\":\"%s\",\"cand\":\"%s\",\"exam\":\"%s\",\"centre\":\"%s\",\"date\":\"%s\",\"hash\":\"%s\"}",
                app.getHallTicketNumber(),
                app.getCandidateId(),
                exam.getCode() != null ? exam.getCode() : exam.getId().toString(),
                centreName,
                examDate,
                app.getQrVerificationHash() != null ? app.getQrVerificationHash().substring(0, 16) : ""
        );

        return AdmitCardResponse.builder()
                .applicationId(app.getId())
                .hallTicketNumber(app.getHallTicketNumber())
                .candidateId(app.getCandidateId())
                .candidateName("Candidate " + app.getCandidateId().toString().substring(0, 8))
                .examId(exam.getId())
                .examName(exam.getName())
                .examCode(exam.getCode())
                .conductingAuthority(exam.getConductingAuthority())
                .examinationMode(exam.getExaminationMode())
                .durationMinutes(exam.getDurationMinutes())
                .totalMarks(exam.getTotalMarks())
                .examDate(examDate)
                .shiftName(shiftName)
                .shiftNumber(shiftNumber)
                .reportingTime(reportingTime)
                .gateClosingTime(gateClosingTime)
                .loginStartTime(loginStartTime)
                .examStartTime(examStartTime)
                .examEndTime(examEndTime)
                .centreId(centre != null ? centre.getId() : null)
                .centreName(centreName)
                .building(building)
                .floor(floor)
                .city(city)
                .state(state)
                .laboratoryIdentifier(labIdentifier)
                .qrData(qrPayload)
                .verificationHash(app.getQrVerificationHash())
                .pwdRequired(app.getPwdRequired())
                .scribeRequired(app.getScribeRequired())
                .instructions(STANDARD_INSTRUCTIONS)
                .build();
    }

    private ExamApplicationResponse toResponse(
            ExamApplication app,
            Examination exam,
            UUID centreId,
            ExaminationSchedule schedule,
            UUID shiftId) {

        String examName = exam != null ? exam.getName() : "Examination";
        String examCode = exam != null ? exam.getCode() : "";
        String conductingAuthority = exam != null ? exam.getConductingAuthority() : "";
        Integer durationMinutes = exam != null ? exam.getDurationMinutes() : 60;
        Integer totalMarks = exam != null ? exam.getTotalMarks() : 100;

        ExaminationCentre centre = centreId != null ? centreRepository.findById(centreId).orElse(null) : null;
        ExamShift shift = shiftId != null ? shiftRepository.findById(shiftId).orElse(null) : null;

        return ExamApplicationResponse.builder()
                .applicationId(app.getId())
                .examId(app.getExaminationId())
                .candidateId(app.getCandidateId())
                .status(app.getStatus())
                .applicationDate(app.getAppliedAt())
                .hallTicketNumber(app.getHallTicketNumber())
                .examName(examName)
                .examCode(examCode)
                .conductingAuthority(conductingAuthority)
                .durationMinutes(durationMinutes)
                .totalMarks(totalMarks)
                .allocatedCentreId(centreId)
                .centreName(centre != null ? centre.getCentreName() : null)
                .city(centre != null ? centre.getCity() : null)
                .state(centre != null ? centre.getState() : null)
                .examDate(schedule != null ? schedule.getExamDate() : null)
                .shiftName(shift != null ? shift.getShiftName() : null)
                .build();
    }
}
