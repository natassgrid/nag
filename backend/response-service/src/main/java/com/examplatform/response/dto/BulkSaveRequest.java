package com.examplatform.response.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request payload for bulk-saving offline-buffered responses.
 * The responses list is ordered — reconciliation processes them sequentially.
 *
 * Validates: Requirements 10.4
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkSaveRequest {

    /**
     * Ordered list of responses buffered offline.
     * Each response includes a revisionSequence for deduplication.
     */
    @NotEmpty(message = "responses list must not be empty")
    @Valid
    private List<SaveResponseRequest> responses;
}
