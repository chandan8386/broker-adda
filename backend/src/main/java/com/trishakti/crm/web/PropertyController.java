package com.trishakti.crm.web;

import com.trishakti.crm.common.PageResponse;
import com.trishakti.crm.domain.enums.PropertyStatus;
import com.trishakti.crm.domain.enums.PropertyType;
import com.trishakti.crm.dto.PropertyDtos.ProjectRequest;
import com.trishakti.crm.dto.PropertyDtos.ProjectResponse;
import com.trishakti.crm.dto.PropertyDtos.PropertyRequest;
import com.trishakti.crm.dto.PropertyDtos.PropertyResponse;
import com.trishakti.crm.service.PropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/properties")
@Tag(name = "Property Management")
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @GetMapping
    @Operation(summary = "Search / filter / paginate properties")
    public PageResponse<PropertyResponse> list(@RequestParam(required = false) String q,
                                               @RequestParam(required = false) PropertyType type,
                                               @RequestParam(required = false) PropertyStatus status,
                                               @PageableDefault(size = 20, sort = "title") Pageable pageable) {
        return propertyService.search(q, type, status, pageable);
    }

    @GetMapping("/{id}")
    public PropertyResponse get(@PathVariable Long id) {
        return propertyService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SALES_MANAGER')")
    public PropertyResponse create(@Valid @RequestBody PropertyRequest request) {
        return propertyService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SALES_MANAGER')")
    public PropertyResponse update(@PathVariable Long id, @Valid @RequestBody PropertyRequest request) {
        return propertyService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        propertyService.delete(id);
    }

    @GetMapping("/projects")
    public List<ProjectResponse> projects() {
        return propertyService.listProjects();
    }

    @PostMapping("/projects")
    @PreAuthorize("hasAnyRole('ADMIN','SALES_MANAGER')")
    public ProjectResponse createProject(@Valid @RequestBody ProjectRequest request) {
        return propertyService.createProject(request);
    }
}
