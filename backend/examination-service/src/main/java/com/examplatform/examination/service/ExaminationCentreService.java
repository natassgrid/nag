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

package com.examplatform.examination.service;

import com.examplatform.examination.domain.ExaminationCentre;
import com.examplatform.examination.domain.ShiftSeatAllocation;
import com.examplatform.examination.dto.schedule.CentreResponse;
import com.examplatform.examination.dto.schedule.CreateCentreRequest;
import com.examplatform.examination.dto.schedule.SeatAllocationRequest;
import com.examplatform.examination.dto.schedule.SeatAllocationResponse;
import com.examplatform.examination.exception.CentreNotFoundException;
import com.examplatform.examination.exception.SeatAllocationException;
import com.examplatform.examination.exception.ShiftNotFoundException;
import com.examplatform.examination.repository.ExaminationCentreRepository;
import com.examplatform.examination.repository.ExamShiftRepository;
import com.examplatform.examination.repository.ShiftSeatAllocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for examination centre management and shift seat allocation.
 * Validates: Requirements 7b.5, 7b.6, 7b.11
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ExaminationCentreService {

    private static final String AUDIT_TOPIC = "exam.audit.events";

    private final ExaminationCentreRepository centreRepository;
    private final ExamShiftRepository shiftRepository;
    private final ShiftSeatAllocationRepository allocationRepository;
    private final GeoLocationService geoLocationService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ── Centres ───────────────────────────────────────────────────────────────

    public CentreResponse createCentre(CreateCentreRequest request, String tenantId) {
        ExaminationCentre centre = ExaminationCentre.builder()
                .countryId(request.getCountryId())
                .stateId(request.getStateId())
                .cityId(request.getCityId())
                .region(request.getRegion())
                .state(request.getState())
                .district(request.getDistrict())
                .city(request.getCity())
                .centreName(request.getCentreName())
                .building(request.getBuilding())
                .floor(request.getFloor())
                .laboratoryIdentifier(request.getLaboratoryIdentifier())
                .totalCapacity(request.getTotalCapacity())
                .active(request.isActive())
                .build();
        centre.setTenantId(tenantId);

        ExaminationCentre saved = centreRepository.save(centre);
        log.info("Centre created: id={}, name='{}', tenant={}", saved.getId(), saved.getCentreName(), tenantId);
        return toCentreResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CentreResponse> listCentres(String tenantId, String state, String city) {
        List<ExaminationCentre> centres;
        if (state != null && !state.isBlank()) {
            centres = centreRepository.findByTenantIdAndStateIgnoreCaseAndActiveTrue(tenantId, state);
        } else if (city != null && !city.isBlank()) {
            centres = centreRepository.findByTenantIdAndCityIgnoreCaseAndActiveTrue(tenantId, city);
        } else {
            centres = centreRepository.findByTenantIdAndActiveTrue(tenantId);
        }
        return centres.stream().map(this::toCentreResponse).toList();
    }

    /**
     * Lists centres with server-side pagination and optional search/filter.
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<CentreResponse> listCentresPaged(
            String tenantId, String search, String state, String city, int page, int size) {
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(page, size,
                        org.springframework.data.domain.Sort.by("createdAt").descending());

        org.springframework.data.domain.Page<ExaminationCentre> centrePage;
        if (search != null && !search.isBlank()) {
            centrePage = centreRepository.findByTenantIdAndCentreNameContainingIgnoreCaseAndActiveTrue(
                    tenantId, search.trim(), pageable);
        } else {
            centrePage = centreRepository.findByTenantIdAndActiveTrue(tenantId, pageable);
        }

        return centrePage.map(this::toCentreResponse);
    }

    @Transactional(readOnly = true)
    public CentreResponse getCentre(UUID centreId, String tenantId) {
        return toCentreResponse(findCentre(centreId, tenantId));
    }

    public CentreResponse deactivateCentre(UUID centreId, String tenantId) {
        ExaminationCentre centre = findCentre(centreId, tenantId);
        centre.setActive(false);
        return toCentreResponse(centreRepository.save(centre));
    }

    // ── Seat Allocation ───────────────────────────────────────────────────────

    /**
     * Creates or replaces the seat allocation for a (shift, centre) pair.
     * Validates that availableSeats >= 0 (Req 7b.6).
     */
    public SeatAllocationResponse upsertAllocation(UUID shiftId,
                                                    SeatAllocationRequest request,
                                                    UUID actorId,
                                                    String tenantId) {
        shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ShiftNotFoundException(shiftId));

        findCentre(request.getCentreId(), tenantId);

        if (request.getAvailableSeats() < 0) {
            throw new SeatAllocationException(
                    "availableSeats must not be negative; got " + request.getAvailableSeats());
        }

        ShiftSeatAllocation allocation = allocationRepository
                .findByShiftIdAndCentreId(shiftId, request.getCentreId())
                .orElseGet(() -> {
                    ShiftSeatAllocation a = ShiftSeatAllocation.builder()
                            .shiftId(shiftId)
                            .centreId(request.getCentreId())
                            .build();
                    a.setTenantId(tenantId);
                    return a;
                });

        allocation.setTotalSeats(request.getTotalSeats());
        allocation.setAvailableSeats(request.getAvailableSeats());
        allocation.setReservedSeats(request.getReservedSeats());
        allocation.setPwdSeats(request.getPwdSeats());
        allocation.setEmergencyBufferSeats(request.getEmergencyBufferSeats());
        allocation.setFemaleReservedSeats(request.getFemaleReservedSeats());
        allocation.setSpecialCategorySeats(request.getSpecialCategorySeats());

        ShiftSeatAllocation saved = allocationRepository.save(allocation);
        log.info("Seat allocation upserted: shift={}, centre={}, available={}",
                shiftId, request.getCentreId(), request.getAvailableSeats());

        publishAuditAllocation(saved, actorId, tenantId);
        return toAllocationResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SeatAllocationResponse> listAllocations(UUID shiftId, String tenantId) {
        shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ShiftNotFoundException(shiftId));
        return allocationRepository.findByShiftId(shiftId)
                .stream().map(this::toAllocationResponse).toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ExaminationCentre findCentre(UUID centreId, String tenantId) {
        ExaminationCentre centre = centreRepository.findById(centreId)
                .orElseThrow(() -> new CentreNotFoundException(centreId));
        if (!tenantId.equals(centre.getTenantId())) {
            throw new CentreNotFoundException(centreId);
        }
        return centre;
    }

    private CentreResponse toCentreResponse(ExaminationCentre c) {
        String countryName = geoLocationService.getCountryById(c.getCountryId())
                .map(g -> g.getName()).orElse(null);

        String stateName = geoLocationService.getStateByCountryIdAndStateId(c.getCountryId(), c.getStateId())
                .map(g -> g.getName()).orElse(c.getState());

        String cityName = geoLocationService.getCityByStateIdAndCityId(c.getStateId(), c.getCityId())
                .map(g -> g.getName()).orElse(c.getCity());

        return CentreResponse.builder()
                .id(c.getId())
                .countryId(c.getCountryId())
                .stateId(c.getStateId())
                .cityId(c.getCityId())
                .countryName(countryName)
                .stateName(stateName)
                .cityName(cityName)
                .region(c.getRegion())
                .state(c.getState())
                .district(c.getDistrict())
                .city(c.getCity())
                .centreName(c.getCentreName())
                .building(c.getBuilding())
                .floor(c.getFloor())
                .laboratoryIdentifier(c.getLaboratoryIdentifier())
                .totalCapacity(c.getTotalCapacity())
                .active(c.isActive())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    private SeatAllocationResponse toAllocationResponse(ShiftSeatAllocation a) {
        return SeatAllocationResponse.builder()
                .id(a.getId())
                .shiftId(a.getShiftId())
                .centreId(a.getCentreId())
                .totalSeats(a.getTotalSeats())
                .availableSeats(a.getAvailableSeats())
                .reservedSeats(a.getReservedSeats())
                .pwdSeats(a.getPwdSeats())
                .emergencyBufferSeats(a.getEmergencyBufferSeats())
                .femaleReservedSeats(a.getFemaleReservedSeats())
                .specialCategorySeats(a.getSpecialCategorySeats())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }

    private void publishAuditAllocation(ShiftSeatAllocation a, UUID actorId, String tenantId) {
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("eventType", "SEAT_ALLOCATION_UPDATED");
            event.put("allocationId", a.getId().toString());
            event.put("shiftId", a.getShiftId().toString());
            event.put("centreId", a.getCentreId().toString());
            event.put("availableSeats", a.getAvailableSeats());
            event.put("actorId", actorId != null ? actorId.toString() : null);
            event.put("tenantId", tenantId);
            event.put("occurredAt", Instant.now().toString());
            kafkaTemplate.send(AUDIT_TOPIC, a.getId().toString(), event)
                    .whenComplete((r, ex) -> {
                        if (ex != null) log.error("Failed to publish SEAT_ALLOCATION_UPDATED: {}", ex.getMessage());
                    });
        } catch (Exception e) {
            log.error("Unexpected error publishing seat allocation audit: {}", e.getMessage());
        }
    }
}
