package com.trishakti.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public final class BulkImportDtos {
    private BulkImportDtos() {}

    public record StartRequest(@NotBlank String fileName, @NotEmpty List<String> headers) {}

    public record StartResponse(String uploadId, int batchSize, String status) {}

    public record ChunkRequest(
            @NotNull @Size(min = 1, max = 1000) List<Map<String, String>> rows,
            @jakarta.validation.constraints.PositiveOrZero int firstRowNumber) {}

    public record ChunkResponse(String uploadId, String status, int receivedRows, int imported,
                                int skipped, int invalid, int duplicate, List<String> errors) {}

    public record StatusResponse(String uploadId, String status, int receivedRows, int imported,
                                 int skipped, int invalid, int duplicate, List<String> errors) {}

    public record HeaderResponse(@NotEmpty List<String> headers) {}
}