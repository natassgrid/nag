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

package com.examplatform.examination.controller;

import com.examplatform.examination.domain.GeoCity;
import com.examplatform.examination.domain.GeoCountry;
import com.examplatform.examination.domain.GeoState;
import com.examplatform.examination.service.GeoLocationService;
import com.examplatform.examination.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("GeoLocationController REST Endpoints E2E Tests (MockMvc)")
class GeoLocationControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private GeoLocationService geoLocationService;

    @Test
    @DisplayName("+ve: Public access to getCountries() returns 200 OK without authentication")
    void publicCanGetCountries() throws Exception {
        GeoCountry india = GeoCountry.builder()
                .id(101L)
                .name("India")
                .iso2("IN")
                .iso3("IND")
                .phoneCode("91")
                .build();

        when(geoLocationService.getCountries()).thenReturn(List.of(india));

        mockMvc.perform(get("/api/v1/geo/countries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[0].id").value(101))
                .andExpect(jsonPath("$.data[0].name").value("India"));
    }

    @Test
    @DisplayName("+ve: Public access to getStates(countryId) returns 200 OK")
    void publicCanGetStates() throws Exception {
        GeoState maharashtra = GeoState.builder()
                .id(201L)
                .name("Maharashtra")
                .stateCode("MH")
                .countryId(101L)
                .build();

        when(geoLocationService.getStatesByCountry(eq(101L))).thenReturn(List.of(maharashtra));

        mockMvc.perform(get("/api/v1/geo/countries/{countryId}/states", 101L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[0].id").value(201))
                .andExpect(jsonPath("$.data[0].name").value("Maharashtra"));
    }

    @Test
    @DisplayName("+ve: Public access to getCities(stateId) returns 200 OK")
    void publicCanGetCities() throws Exception {
        GeoCity mumbai = GeoCity.builder()
                .id(301L)
                .name("Mumbai")
                .stateId(201L)
                .build();

        when(geoLocationService.getCitiesByState(eq(201L))).thenReturn(List.of(mumbai));

        mockMvc.perform(get("/api/v1/geo/states/{stateId}/cities", 201L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[0].id").value(301))
                .andExpect(jsonPath("$.data[0].name").value("Mumbai"));
    }
}
