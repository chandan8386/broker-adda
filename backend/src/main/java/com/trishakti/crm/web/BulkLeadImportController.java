package com.trishakti.crm.web;

import com.trishakti.crm.dto.BulkImportDtos.ChunkRequest;
import com.trishakti.crm.dto.BulkImportDtos.ChunkResponse;
import com.trishakti.crm.dto.BulkImportDtos.StartRequest;
import com.trishakti.crm.dto.BulkImportDtos.StartResponse;
import com.trishakti.crm.dto.BulkImportDtos.StatusResponse;
import com.trishakti.crm.service.BulkLeadImportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/leads/imports")
@Tag(name = "Lead Management")
public class BulkLeadImportController {
    private final BulkLeadImportService service;

    public BulkLeadImportController(BulkLeadImportService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StartResponse start(@Valid @RequestBody StartRequest request) { return service.start(request.fileName(), request.headers()); }

    @PostMapping("/{uploadId}/chunks")
    public ChunkResponse chunk(@PathVariable String uploadId, @Valid @RequestBody ChunkRequest request) {
        return service.addChunk(uploadId, request);
    }

    @PostMapping("/{uploadId}/complete")
    public StatusResponse complete(@PathVariable String uploadId) { return service.complete(uploadId); }

    @GetMapping("/{uploadId}")
    public StatusResponse status(@PathVariable String uploadId) { return service.status(uploadId); }
}