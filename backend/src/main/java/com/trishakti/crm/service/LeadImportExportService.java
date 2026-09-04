package com.trishakti.crm.service;

import com.trishakti.crm.domain.Lead;
import com.trishakti.crm.domain.User;
import com.trishakti.crm.domain.enums.ActivityType;
import com.trishakti.crm.domain.enums.LeadPriority;
import com.trishakti.crm.domain.enums.LeadStatus;
import com.trishakti.crm.domain.enums.PropertyType;
import com.trishakti.crm.domain.enums.SourceChannel;
import com.trishakti.crm.dto.LeadDtos.ImportResultResponse;
import com.trishakti.crm.exception.DomainExceptions.BusinessRuleException;
import com.trishakti.crm.exception.DomainExceptions.ResourceNotFoundException;
import com.trishakti.crm.repository.LeadRepository;
import com.trishakti.crm.repository.UserRepository;
import com.trishakti.crm.security.SecurityUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class LeadImportExportService {

    /** CSV header order used for both import template and export. */
    public static final String[] HEADERS = {
            "customerName", "mobile", "email", "source", "sourceChannel", "propertyType",
            "budgetMin", "budgetMax", "preferredLocation", "priority", "notes"
    };

    private final LeadRepository leadRepository;
    private final UserRepository userRepository;
    private final LeadService leadService;

    @PersistenceContext
    private EntityManager entityManager;

    public LeadImportExportService(LeadRepository leadRepository, UserRepository userRepository,
                                   LeadService leadService) {
        this.leadRepository = leadRepository;
        this.userRepository = userRepository;
        this.leadService = leadService;
    }

    @Transactional
    public ImportResultResponse importCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Upload a non-empty CSV file");
        }
        User creator = currentUser();
        List<String> errors = new ArrayList<>();
        int total = 0, imported = 0, skipped = 0;

        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(true).setTrim(true).build()
                     .parse(reader)) {

            for (CSVRecord record : parser) {
                total++;
                long rowNo = record.getRecordNumber() + 1;
                try {
                    String name = get(record, "customerName");
                    String mobile = get(record, "mobile");
                    if (name.isBlank() || mobile.isBlank()) {
                        throw new IllegalArgumentException("customerName and mobile are required");
                    }
                    if (leadRepository.existsByMobileAndDeletedFalse(mobile)) {
                        skipped++;
                        errors.add("Row " + rowNo + ": skipped, mobile " + mobile + " already exists");
                        continue;
                    }
                    Lead lead = new Lead();
                    lead.setCustomerName(name);
                    lead.setMobile(mobile);
                    lead.setEmail(emptyToNull(get(record, "email")));
                    lead.setSource(emptyToNull(get(record, "source")));
                    lead.setSourceChannel(parseEnum(SourceChannel.class, get(record, "sourceChannel"), SourceChannel.OTHER));
                    lead.setPropertyType(parseEnum(PropertyType.class, get(record, "propertyType"), null));
                    lead.setBudgetMin(parseMoney(get(record, "budgetMin")));
                    lead.setBudgetMax(parseMoney(get(record, "budgetMax")));
                    lead.setPreferredLocation(emptyToNull(get(record, "preferredLocation")));
                    lead.setPriority(parseEnum(LeadPriority.class, get(record, "priority"), LeadPriority.WARM));
                    lead.setNotes(emptyToNull(get(record, "notes")));
                    lead.setStatus(LeadStatus.NEW);
                    lead.setCreatedByUser(creator);
                    leadRepository.save(lead);
                    leadService.logActivity(lead, ActivityType.IMPORT, null, LeadStatus.NEW,
                            "Imported from CSV", null, creator);
                    imported++;
                } catch (Exception rowEx) {
                    skipped++;
                    errors.add("Row " + rowNo + ": " + rowEx.getMessage());
                }
            }
        } catch (IOException e) {
            throw new BusinessRuleException("Could not read CSV: " + e.getMessage());
        }
        return new ImportResultResponse(total, imported, skipped, errors);
    }

    /** Rows pulled per round-trip while streaming an export. */
    private static final int EXPORT_PAGE_SIZE = 1000;

    /**
     * Streams matching leads as CSV straight to {@code writer}, paging through the result set so
     * memory stays flat regardless of row count.
     *
     * <p>Runs inside a read-only transaction: the assignee is read per row, so the entities must
     * stay managed (open-in-view is off). {@code scopeUserId} must be resolved on the request
     * thread via {@link com.trishakti.crm.security.SecurityUtils#currentScopeUserId()} — a
     * streaming response executes on a pool thread with no SecurityContext.
     */
    @Transactional(readOnly = true)
    public long streamCsv(Writer writer, Long scopeUserId, String q, LeadStatus status,
                          SourceChannel sourceChannel, PropertyType propertyType, LeadPriority priority,
                          Long assignedUserId) {

        Specification<Lead> spec = LeadSpecifications.allOf(
                LeadSpecifications.notDeleted(),
                LeadSpecifications.text(q),
                LeadSpecifications.status(status),
                LeadSpecifications.sourceChannel(sourceChannel),
                LeadSpecifications.propertyType(propertyType),
                LeadSpecifications.priority(priority),
                LeadSpecifications.assignedTo(assignedUserId),
                LeadSpecifications.ownedBy(scopeUserId));

        DateTimeFormatter fmt = DateTimeFormatter.ISO_INSTANT;
        long written = 0;
        try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                .setHeader(concat(HEADERS, "reference", "status", "assignedUser", "createdAt")).build())) {

            Page<Lead> chunk;
            int page = 0;
            do {
                chunk = leadRepository.findAll(spec, PageRequest.of(page++, EXPORT_PAGE_SIZE, Sort.by("id")));
                for (Lead l : chunk) {
                    printer.printRecord(
                            l.getCustomerName(), l.getMobile(), l.getEmail(), l.getSource(),
                            l.getSourceChannel(), l.getPropertyType(), l.getBudgetMin(), l.getBudgetMax(),
                            l.getPreferredLocation(), l.getPriority(), l.getNotes(),
                            l.getReference(), l.getStatus(),
                            l.getAssignedUser() != null ? l.getAssignedUser().getFullName() : "",
                            l.getCreatedAt() != null ? fmt.format(l.getCreatedAt()) : "");
                    written++;
                }
                printer.flush();
                // release the chunk so the persistence context does not grow across pages
                entityManager.clear();
            } while (!chunk.isLast());
        } catch (IOException e) {
            throw new BusinessRuleException("Could not generate CSV: " + e.getMessage());
        }
        return written;
    }

    public byte[] template() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(out, StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.builder().setHeader(HEADERS).build())) {
            printer.printRecord("Ramesh Kumar", "9812345678", "ramesh@example.com", "Facebook Diwali Campaign",
                    "FACEBOOK", "FLAT", "4000000", "6000000", "Whitefield", "HOT", "Wants 3BHK, ready to visit");
            printer.flush();
        } catch (IOException e) {
            throw new BusinessRuleException(e.getMessage());
        }
        return out.toByteArray();
    }

    private String get(CSVRecord r, String col) {
        return r.isMapped(col) && r.get(col) != null ? r.get(col).trim() : "";
    }

    private String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private BigDecimal parseMoney(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return new BigDecimal(s.replaceAll("[,\\s₹]", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number: " + s);
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String s, E fallback) {
        if (s == null || s.isBlank()) return fallback;
        try {
            return Enum.valueOf(type, s.trim().toUpperCase().replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private String[] concat(String[] base, String... more) {
        String[] all = new String[base.length + more.length];
        System.arraycopy(base, 0, all, 0, base.length);
        System.arraycopy(more, 0, all, base.length, more.length);
        return all;
    }

    private User currentUser() {
        Long uid = SecurityUtils.currentUserId();
        return userRepository.findById(uid).orElseThrow(() -> new ResourceNotFoundException("User", uid));
    }
}
