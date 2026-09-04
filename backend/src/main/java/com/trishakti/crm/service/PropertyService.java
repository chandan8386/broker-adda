package com.trishakti.crm.service;

import com.trishakti.crm.common.PageResponse;
import com.trishakti.crm.domain.Property;
import com.trishakti.crm.domain.PropertyProject;
import com.trishakti.crm.domain.enums.PropertyStatus;
import com.trishakti.crm.domain.enums.PropertyType;
import com.trishakti.crm.dto.PropertyDtos.ProjectRequest;
import com.trishakti.crm.dto.PropertyDtos.ProjectResponse;
import com.trishakti.crm.dto.PropertyDtos.PropertyRequest;
import com.trishakti.crm.dto.PropertyDtos.PropertyResponse;
import com.trishakti.crm.exception.DomainExceptions.ResourceNotFoundException;
import com.trishakti.crm.mapper.CrmMappers;
import com.trishakti.crm.repository.PropertyProjectRepository;
import com.trishakti.crm.repository.PropertyRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final PropertyProjectRepository projectRepository;

    public PropertyService(PropertyRepository propertyRepository, PropertyProjectRepository projectRepository) {
        this.propertyRepository = propertyRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<PropertyResponse> search(String q, PropertyType type, PropertyStatus status, Pageable pageable) {
        return PageResponse.of(propertyRepository.search(q, type, status, pageable), CrmMappers::property);
    }

    @Transactional(readOnly = true)
    public PropertyResponse get(Long id) {
        return CrmMappers.property(find(id));
    }

    @Transactional
    public PropertyResponse create(PropertyRequest req) {
        Property p = new Property();
        apply(p, req);
        propertyRepository.save(p);
        return CrmMappers.property(p);
    }

    @Transactional
    public PropertyResponse update(Long id, PropertyRequest req) {
        Property p = find(id);
        apply(p, req);
        return CrmMappers.property(p);
    }

    @Transactional
    public void delete(Long id) {
        if (!propertyRepository.existsById(id)) throw new ResourceNotFoundException("Property", id);
        propertyRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listProjects() {
        return projectRepository.findAll().stream().map(CrmMappers::project).toList();
    }

    @Transactional
    public ProjectResponse createProject(ProjectRequest req) {
        PropertyProject project = new PropertyProject();
        project.setName(req.name());
        project.setLocation(req.location());
        project.setCity(req.city());
        project.setDescription(req.description());
        project.setActive(req.active() == null || req.active());
        projectRepository.save(project);
        return CrmMappers.project(project);
    }

    private void apply(Property p, PropertyRequest req) {
        p.setTitle(req.title());
        p.setPropertyType(req.propertyType());
        p.setUnitNumber(req.unitNumber());
        p.setLocation(req.location());
        p.setCity(req.city());
        p.setAreaSqft(req.areaSqft());
        p.setPrice(req.price());
        p.setBedrooms(req.bedrooms());
        p.setBathrooms(req.bathrooms());
        p.setFacing(req.facing());
        if (req.status() != null) p.setStatus(req.status());
        p.setDescription(req.description());
        if (req.projectId() != null) {
            p.setProject(projectRepository.findById(req.projectId())
                    .orElseThrow(() -> new ResourceNotFoundException("PropertyProject", req.projectId())));
        }
    }

    private Property find(Long id) {
        return propertyRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Property", id));
    }
}
