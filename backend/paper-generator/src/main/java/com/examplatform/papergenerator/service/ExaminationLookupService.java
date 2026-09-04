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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Service to resolve human-readable Examination and Shift names from the examination_service schema.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExaminationLookupService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Resolves examination names for given exam UUIDs.
     *
     * @param examIds set of examination IDs
     * @return map of exam ID to exam name
     */
    public Map<UUID, String> findExamNames(Set<UUID> examIds) {
        if (examIds == null || examIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<UUID, String> result = new HashMap<>();
        try {
            String inSql = String.join(",", Collections.nCopies(examIds.size(), "?"));
            String sql = "SELECT id, name FROM examination_service.examination WHERE id IN (" + inSql + ")";
            jdbcTemplate.query(sql, ps -> {
                int idx = 1;
                for (UUID id : examIds) {
                    ps.setObject(idx++, id);
                }
            }, rs -> {
                UUID id = (UUID) rs.getObject("id");
                String name = rs.getString("name");
                if (id != null && name != null) {
                    result.put(id, name);
                }
            });
        } catch (Exception e) {
            log.debug("Could not query examination_service.examination for exam names: {}", e.getMessage());
        }
        return result;
    }

    /**
     * Resolves shift names/numbers for given shift identifier strings.
     *
     * @param shiftIds set of shift ID strings
     * @return map of shift ID string to human-readable shift name
     */
    public Map<String, String> findShiftNames(Set<String> shiftIds) {
        if (shiftIds == null || shiftIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> result = new HashMap<>();
        List<UUID> uuidShifts = new ArrayList<>();
        for (String s : shiftIds) {
            if (s != null && !s.isBlank()) {
                try {
                    uuidShifts.add(UUID.fromString(s));
                } catch (Exception ignored) {
                }
            }
        }

        if (!uuidShifts.isEmpty()) {
            try {
                String inSql = String.join(",", Collections.nCopies(uuidShifts.size(), "?"));
                String sql = "SELECT id, shift_name, shift_number FROM examination_service.exam_shift WHERE id IN (" + inSql + ")";
                jdbcTemplate.query(sql, ps -> {
                    int idx = 1;
                    for (UUID id : uuidShifts) {
                        ps.setObject(idx++, id);
                    }
                }, rs -> {
                    Object idObj = rs.getObject("id");
                    if (idObj != null) {
                        String idStr = idObj.toString();
                        String shiftName = rs.getString("shift_name");
                        int shiftNumber = rs.getInt("shift_number");
                        String label = (shiftName != null && !shiftName.isBlank())
                                ? shiftName
                                : "Shift " + shiftNumber;
                        result.put(idStr, label);
                    }
                });
            } catch (Exception e) {
                log.debug("Could not query examination_service.exam_shift for shift names: {}", e.getMessage());
            }
        }
        return result;
    }
}
