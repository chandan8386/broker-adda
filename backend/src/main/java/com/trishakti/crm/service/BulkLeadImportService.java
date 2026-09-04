package com.trishakti.crm.service;

import com.trishakti.crm.domain.Lead;
import com.trishakti.crm.domain.User;
import com.trishakti.crm.domain.enums.LeadPriority;
import com.trishakti.crm.domain.enums.LeadStatus;
import com.trishakti.crm.domain.enums.PropertyType;
import com.trishakti.crm.domain.enums.SourceChannel;
import com.trishakti.crm.dto.BulkImportDtos.ChunkRequest;
import com.trishakti.crm.dto.BulkImportDtos.ChunkResponse;
import com.trishakti.crm.dto.BulkImportDtos.StartResponse;
import com.trishakti.crm.dto.BulkImportDtos.StatusResponse;
import com.trishakti.crm.exception.DomainExceptions.BusinessRuleException;
import com.trishakti.crm.exception.DomainExceptions.ResourceNotFoundException;
import com.trishakti.crm.repository.LeadRepository;
import com.trishakti.crm.repository.UserRepository;
import com.trishakti.crm.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BulkLeadImportService {
    public static final int BATCH_SIZE = 500;
    private static final int MAX_REPORTED_ERRORS = 1000;
    private static final List<String> REQUIRED_HEADERS = List.of(
            "customerName", "mobile", "email", "source", "sourceChannel", "propertyType",
            "budgetMin", "budgetMax", "preferredLocation", "priority", "notes");

    private final LeadRepository leadRepository;
    private final UserRepository userRepository;
    private final Map<String, Job> jobs = new ConcurrentHashMap<>();

    public BulkLeadImportService(LeadRepository leadRepository, UserRepository userRepository) {
        this.leadRepository = leadRepository;
        this.userRepository = userRepository;
    }

    public StartResponse start(String fileName, List<String> headers) {
        if (fileName == null || fileName.isBlank() || !fileName.toLowerCase().endsWith(".csv")) {
            throw new BusinessRuleException("Upload a CSV file");
        }
        if (headers == null || !new HashSet<>(headers).containsAll(REQUIRED_HEADERS)) {
            throw new BusinessRuleException("CSV headers must include: " + String.join(", ", REQUIRED_HEADERS));
        }
        Long userId = SecurityUtils.currentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        String id = UUID.randomUUID().toString();
        jobs.put(id, new Job(user));
        return new StartResponse(id, BATCH_SIZE, "UPLOADING");
    }

    @Transactional
    public synchronized ChunkResponse addChunk(String id, ChunkRequest request) {
        Job job = ownedJob(id);
        if ("COMPLETED".equals(job.status) || "FAILED".equals(job.status)) {
            throw new BusinessRuleException("Import job is already " + job.status.toLowerCase());
        }
        if (request.firstRowNumber() != job.nextRowNumber) {
            throw new BusinessRuleException("Unexpected first row number; expected " + job.nextRowNumber);
        }
        if (request.rows().size() > BATCH_SIZE) {
            throw new BusinessRuleException("A chunk cannot contain more than " + BATCH_SIZE + " rows");
        }
        job.status = "PROCESSING";
        Set<String> mobiles = request.rows().stream().map(row -> value(row, "mobile"))
                .filter(value -> !value.isBlank()).collect(java.util.stream.Collectors.toSet());
        Set<String> existing = mobiles.isEmpty()
            ? Set.of()
            : new HashSet<>(leadRepository.findExistingMobiles(mobiles));
        Set<String> seenInChunk = new HashSet<>();
        List<Lead> valid = new ArrayList<>(request.rows().size());
        for (int index = 0; index < request.rows().size(); index++) {
            Map<String, String> row = request.rows().get(index);
            int rowNumber = request.firstRowNumber() + index;
            try {
                String mobile = value(row, "mobile");
                String name = value(row, "customerName");
                if (name.isBlank() || mobile.isBlank()) throw new IllegalArgumentException("customerName and mobile are required");
                if (!mobile.matches("^[0-9+\\-\\s]{7,20}$")) throw new IllegalArgumentException("Invalid mobile number");
                if (existing.contains(mobile) || !seenInChunk.add(mobile)) {
                    job.duplicate++;
                    job.skipped++;
                    error(job, rowNumber + ": duplicate mobile " + mobile);
                    continue;
                }
                Lead lead = new Lead();
                lead.setCustomerName(name);
                lead.setMobile(mobile);
                lead.setEmail(nullable(row, "email"));
                lead.setSource(nullable(row, "source"));
                lead.setSourceChannel(enumValue(SourceChannel.class, row, "sourceChannel", SourceChannel.OTHER));
                lead.setPropertyType(enumValue(PropertyType.class, row, "propertyType", null));
                lead.setBudgetMin(money(value(row, "budgetMin")));
                lead.setBudgetMax(money(value(row, "budgetMax")));
                lead.setPreferredLocation(nullable(row, "preferredLocation"));
                lead.setPriority(enumValue(LeadPriority.class, row, "priority", LeadPriority.WARM));
                lead.setNotes(nullable(row, "notes"));
                lead.setStatus(LeadStatus.NEW);
                lead.setCreatedByUser(job.user);
                valid.add(lead);
            } catch (RuntimeException ex) {
                job.invalid++;
                job.skipped++;
                error(job, rowNumber + ": " + ex.getMessage());
            }
        }
        leadRepository.saveAll(valid);
        job.received += request.rows().size();
        job.imported += valid.size();
        job.nextRowNumber += request.rows().size();
        return response(id, job);
    }

    public synchronized StatusResponse complete(String id) {
        Job job = ownedJob(id);
        if ("COMPLETED".equals(job.status)) return status(id, job);
        job.status = "COMPLETED";
        return status(id, job);
    }

    public StatusResponse status(String id) {
        Job job = ownedJob(id);
        return status(id, job);
    }

    private StatusResponse status(String id, Job job) {
        return new StatusResponse(id, job.status, job.received, job.imported, job.skipped,
                job.invalid, job.duplicate, List.copyOf(job.errors));
    }

    private ChunkResponse response(String id, Job job) {
        return new ChunkResponse(id, job.status, job.received, job.imported, job.skipped,
                job.invalid, job.duplicate, List.copyOf(job.errors));
    }

    private Job ownedJob(String id) {
        Job job = jobs.get(id);
        if (job == null || !job.user.getId().equals(SecurityUtils.currentUserId())) {
            throw new ResourceNotFoundException("Import job", id);
        }
        return job;
    }

    private void error(Job job, String message) {
        if (job.errors.size() < MAX_REPORTED_ERRORS) job.errors.add(message);
    }

    private static String value(Map<String, String> row, String key) {
        return row.getOrDefault(key, "").trim();
    }

    private static String nullable(Map<String, String> row, String key) {
        String value = value(row, key);
        return value.isBlank() ? null : value;
    }

    private static BigDecimal money(String value) {
        if (value.isBlank()) return null;
        try { return new BigDecimal(value.replaceAll("[,\\s₹]", "")); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("Invalid number: " + value); }
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, Map<String, String> row, String key, E fallback) {
        String value = value(row, key);
        if (value.isBlank()) return fallback;
        try { return Enum.valueOf(type, value.toUpperCase().replace(' ', '_')); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Invalid " + key + ": " + value); }
    }

    public static List<String> requiredHeaders() { return REQUIRED_HEADERS; }

    private static final class Job {
        private final User user;
        private String status = "UPLOADING";
        private int received;
        private int imported;
        private int skipped;
        private int invalid;
        private int duplicate;
        private int nextRowNumber = 2;
        private final List<String> errors = new ArrayList<>();

        private Job(User user) { this.user = user; }
    }
}