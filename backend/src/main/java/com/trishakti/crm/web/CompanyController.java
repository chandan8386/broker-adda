package com.trishakti.crm.web;

import com.trishakti.crm.domain.enums.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/company")
@Tag(name = "Company & Metadata")
public class CompanyController {

    @Value("${app.company.name}")
    private String companyName;

    @GetMapping
    @Operation(summary = "Public company info + enum vocabularies for dropdowns")
    public Map<String, Object> info() {
        return Map.of(
                "name", companyName,
                "product", "Property Lead Management / CRM",
                "enums", Map.ofEntries(
                        Map.entry("leadStatus", names(LeadStatus.values())),
                        Map.entry("leadPriority", names(LeadPriority.values())),
                        Map.entry("sourceChannel", names(SourceChannel.values())),
                        Map.entry("propertyType", names(PropertyType.values())),
                        Map.entry("propertyStatus", names(PropertyStatus.values())),
                        Map.entry("callOutcome", names(CallOutcome.values())),
                        Map.entry("callDisposition", names(CallDisposition.values())),
                        Map.entry("siteVisitStatus", names(SiteVisitStatus.values())),
                        Map.entry("siteVisitResult", names(SiteVisitResult.values())),
                        Map.entry("bookingStatus", names(BookingStatus.values())),
                        Map.entry("purchaseStatus", names(PurchaseStatus.values())),
                        Map.entry("paymentType", names(PaymentType.values())),
                        Map.entry("paymentMode", names(PaymentMode.values())),
                        Map.entry("paymentStatus", names(PaymentStatus.values())),
                        Map.entry("taskType", names(TaskType.values())),
                        Map.entry("taskPriority", names(TaskPriority.values())),
                        Map.entry("taskStatus", names(TaskStatus.values())),
                        Map.entry("role", names(RoleName.values()))),
                "leadWorkflow", Arrays.stream(LeadStatus.values())
                        .collect(Collectors.toMap(Enum::name, s -> names(s.allowedNext().toArray(Enum[]::new)))));
    }

    private List<String> names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toList();
    }
}
