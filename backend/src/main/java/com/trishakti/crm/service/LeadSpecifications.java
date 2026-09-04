package com.trishakti.crm.service;

import com.trishakti.crm.domain.Lead;
import com.trishakti.crm.domain.enums.LeadPriority;
import com.trishakti.crm.domain.enums.LeadStatus;
import com.trishakti.crm.domain.enums.PropertyType;
import com.trishakti.crm.domain.enums.SourceChannel;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;

public final class LeadSpecifications {

    private LeadSpecifications() {}

    public static Specification<Lead> notDeleted() {
        return (root, q, cb) -> cb.isFalse(root.get("deleted"));
    }

    public static Specification<Lead> text(String term) {
        if (term == null || term.isBlank()) return null;
        String like = "%" + term.toLowerCase() + "%";
        return (root, q, cb) -> cb.or(
                cb.like(cb.lower(root.get("customerName")), like),
                cb.like(cb.lower(root.get("mobile")), like),
                cb.like(cb.lower(root.get("email")), like),
                cb.like(cb.lower(root.get("preferredLocation")), like),
                cb.like(cb.lower(root.get("source")), like));
    }

    public static Specification<Lead> status(LeadStatus status) {
        return status == null ? null : (root, q, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Lead> sourceChannel(SourceChannel channel) {
        return channel == null ? null : (root, q, cb) -> cb.equal(root.get("sourceChannel"), channel);
    }

    public static Specification<Lead> propertyType(PropertyType type) {
        return type == null ? null : (root, q, cb) -> cb.equal(root.get("propertyType"), type);
    }

    public static Specification<Lead> priority(LeadPriority priority) {
        return priority == null ? null : (root, q, cb) -> cb.equal(root.get("priority"), priority);
    }

    public static Specification<Lead> assignedTo(Long userId) {
        return userId == null ? null : (root, q, cb) -> cb.equal(root.get("assignedUser").get("id"), userId);
    }

    public static Specification<Lead> unassigned(Boolean unassigned) {
        if (unassigned == null || !unassigned) return null;
        return (root, q, cb) -> cb.isNull(root.get("assignedUser"));
    }

    public static Specification<Lead> budgetAtLeast(BigDecimal min) {
        return min == null ? null : (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("budgetMax"), min);
    }

    public static Specification<Lead> budgetAtMost(BigDecimal max) {
        return max == null ? null : (root, q, cb) -> cb.lessThanOrEqualTo(root.get("budgetMin"), max);
    }

    public static Specification<Lead> createdBetween(Instant from, Instant to) {
        if (from == null && to == null) return null;
        return (root, q, cb) -> {
            if (from != null && to != null) return cb.between(root.get("createdAt"), from, to);
            if (from != null) return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            return cb.lessThanOrEqualTo(root.get("createdAt"), to);
        };
    }

    public static Specification<Lead> followUpBetween(Instant from, Instant to) {
        if (from == null && to == null) return null;
        return (root, q, cb) -> {
            if (from != null && to != null) return cb.between(root.get("nextFollowUpAt"), from, to);
            if (from != null) return cb.greaterThanOrEqualTo(root.get("nextFollowUpAt"), from);
            return cb.lessThanOrEqualTo(root.get("nextFollowUpAt"), to);
        };
    }

    /**
     * Row-level scoping: calling team / sales executives only see leads they are assigned to or
     * created. Pass {@code null} (admins & managers) for no restriction.
     */
    public static Specification<Lead> ownedBy(Long userId) {
        if (userId == null) return null;
        return (root, query, cb) -> cb.or(
                cb.equal(root.get("assignedUser").get("id"), userId),
                cb.equal(root.get("createdByUser").get("id"), userId));
    }

    /** Combines the non-null specs with AND. Specification.where(null) is deprecated in Spring Data 3.5. */
    @SafeVarargs
    public static Specification<Lead> allOf(Specification<Lead>... specs) {
        Specification<Lead> result = null;
        for (Specification<Lead> spec : specs) {
            if (spec == null) continue;
            result = (result == null) ? spec : result.and(spec);
        }
        return result != null ? result : (root, query, cb) -> cb.conjunction();
    }
}
