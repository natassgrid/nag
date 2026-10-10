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

package com.examplatform.shared.error;

import com.examplatform.shared.api.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionHandlerBaseClassesTest {

    static class SampleApiResponseHandler extends BaseApiResponseExceptionHandler {}
    static class SampleProblemDetailHandler extends BaseProblemDetailExceptionHandler {}

    private final SampleApiResponseHandler apiResponseHandler = new SampleApiResponseHandler();
    private final SampleProblemDetailHandler problemDetailHandler = new SampleProblemDetailHandler();

    @Test
    @DisplayName("BaseApiResponseExceptionHandler handles various common exceptions correctly")
    void testApiResponseHandler() throws NoSuchMethodException {
        // AccessDenied
        ResponseEntity<ApiResponse<Void>> deniedResp = apiResponseHandler.handleAccessDenied(new AccessDeniedException("Forbidden"));
        assertThat(deniedResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(deniedResp.getBody().getMessage()).isEqualTo("Access denied");

        // IllegalArgument
        ResponseEntity<ApiResponse<Void>> illegalArgResp = apiResponseHandler.handleIllegalArgument(new IllegalArgumentException("Bad input"));
        assertThat(illegalArgResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(illegalArgResp.getBody().getMessage()).isEqualTo("Bad input");

        // Generic Exception
        ResponseEntity<ApiResponse<Void>> genericResp = apiResponseHandler.handleGenericException(new RuntimeException("Boom"));
        assertThat(genericResp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        // Missing Header
        ResponseEntity<ApiResponse<Void>> headerResp = apiResponseHandler.handleMissingHeader(
                new MissingRequestHeaderException("X-Tenant-ID", new MethodParameter(getClass().getDeclaredMethod("testApiResponseHandler"), -1)));
        assertThat(headerResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(headerResp.getBody().getMessage()).contains("X-Tenant-ID");

        // Type Mismatch
        ResponseEntity<ApiResponse<Void>> mismatchResp = apiResponseHandler.handleTypeMismatch(
                new MethodArgumentTypeMismatchException("abc", Integer.class, "page", null, null));
        assertThat(mismatchResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(mismatchResp.getBody().getMessage()).contains("page");

        // Validation
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "test");
        bindingResult.addError(new FieldError("test", "name", "Name is required"));
        MethodArgumentNotValidException valEx = new MethodArgumentNotValidException(
                new MethodParameter(getClass().getDeclaredMethod("testApiResponseHandler"), -1), bindingResult);
        ResponseEntity<ApiResponse<Void>> valResp = apiResponseHandler.handleValidationException(valEx);
        assertThat(valResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(valResp.getBody().getMessage()).contains("Name is required");
    }

    @Test
    @DisplayName("BaseProblemDetailExceptionHandler handles various common exceptions correctly")
    void testProblemDetailHandler() throws NoSuchMethodException {
        // AccessDenied
        ProblemDetail deniedPd = problemDetailHandler.handleAccessDenied(new AccessDeniedException("Denied"));
        assertThat(deniedPd.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(deniedPd.getTitle()).isEqualTo("Forbidden");

        // IllegalArgument
        ProblemDetail notFoundPd = problemDetailHandler.handleIllegalArgument(new IllegalArgumentException("Not found item"));
        assertThat(notFoundPd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(notFoundPd.getTitle()).isEqualTo("Resource Not Found");

        // Generic Exception
        ProblemDetail genericPd = problemDetailHandler.handleGenericException(new RuntimeException("Crash"));
        assertThat(genericPd.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(genericPd.getTitle()).isEqualTo("Internal Server Error");

        // Validation
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "test");
        bindingResult.addError(new FieldError("test", "name", "Invalid name"));
        MethodArgumentNotValidException valEx = new MethodArgumentNotValidException(
                new MethodParameter(getClass().getDeclaredMethod("testProblemDetailHandler"), -1), bindingResult);
        ProblemDetail valPd = problemDetailHandler.handleValidationError(valEx);
        assertThat(valPd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(valPd.getTitle()).isEqualTo("Bad Request");
    }
}
