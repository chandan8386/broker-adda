package com.trishakti.crm.config;

import com.trishakti.crm.domain.*;
import com.trishakti.crm.domain.enums.*;
import com.trishakti.crm.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Idempotent seed data for demos & tests. Runs once when the user table is empty.
 * Disable with SEED_ENABLED=false.
 */
@Configuration
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String DEFAULT_PASSWORD = "Password@123";

    @Bean
    @Order(2)
    ApplicationRunner seed(RoleRepository roleRepo, UserRepository userRepo, TeamRepository teamRepo,
                           PropertyProjectRepository projectRepo, PropertyRepository propertyRepo,
                           LeadRepository leadRepo, LeadActivityRepository activityRepo,
                           CallLogRepository callRepo, SiteVisitRepository visitRepo,
                           PasswordEncoder encoder) {
        return args -> {
            // Roles are guaranteed by RoleInitializer (@Order 1).
            if (userRepo.count() > 0) {
                log.info("Seed skipped: {} user(s) already present", userRepo.count());
                return;
            }
            log.info("Seeding demo data for Shri Trishakti Infra Realtors Pvt Ltd ...");

            Role admin = roleRepo.findByName(RoleName.ADMIN).orElseThrow();
            Role manager = roleRepo.findByName(RoleName.SALES_MANAGER).orElseThrow();
            Role caller = roleRepo.findByName(RoleName.CALLING_TEAM).orElseThrow();
            Role sales = roleRepo.findByName(RoleName.SALES_EXECUTIVE).orElseThrow();

            Team callingTeam = team(teamRepo, "Calling Team A", "Outbound calling for advertisement leads");
            Team salesTeam = team(teamRepo, "Sales Team A", "Site visits, negotiation & closures");

            User adminUser = user(userRepo, encoder, "System Administrator", "admin", "admin@trishakti.com",
                    "9000000001", null, Set.of(admin));
            User mgr = user(userRepo, encoder, "Priya Sharma", "manager", "manager@trishakti.com",
                    "9000000002", null, Set.of(manager));
            User caller1 = user(userRepo, encoder, "Amit Verma", "caller1", "caller1@trishakti.com",
                    "9000000003", callingTeam, Set.of(caller));
            User caller2 = user(userRepo, encoder, "Sneha Patel", "caller2", "caller2@trishakti.com",
                    "9000000004", callingTeam, Set.of(caller));
            User sales1 = user(userRepo, encoder, "Rahul Nair", "sales1", "sales1@trishakti.com",
                    "9000000005", salesTeam, Set.of(sales));
            User sales2 = user(userRepo, encoder, "Kiran Rao", "sales2", "sales2@trishakti.com",
                    "9000000006", salesTeam, Set.of(sales));

            callingTeam.setManager(mgr);
            salesTeam.setManager(mgr);
            caller1.setManager(mgr);
            caller2.setManager(mgr);
            sales1.setManager(mgr);
            sales2.setManager(mgr);

            PropertyProject green = projectRepo.save(project("Trishakti Green Villas", "Sarjapur Road", "Bengaluru",
                    "Premium 3 & 4 BHK villas with clubhouse"));
            PropertyProject sky = projectRepo.save(project("Trishakti Sky Residences", "Hinjewadi", "Pune",
                    "2 & 3 BHK high-rise apartments"));
            PropertyProject biz = projectRepo.save(project("Trishakti Business Hub", "Gachibowli", "Hyderabad",
                    "Grade-A office spaces & retail"));

            propertyRepo.saveAll(List.of(
                    property(green, "Green Villas - Unit A1", PropertyType.VILLA, "A1", "Sarjapur Road", "Bengaluru",
                            "2400", "18500000", 3, 3, PropertyStatus.AVAILABLE),
                    property(green, "Green Villas - Unit A2", PropertyType.VILLA, "A2", "Sarjapur Road", "Bengaluru",
                            "3000", "23000000", 4, 4, PropertyStatus.AVAILABLE),
                    property(sky, "Sky Residences - 1204", PropertyType.FLAT, "1204", "Hinjewadi", "Pune",
                            "1250", "9800000", 2, 2, PropertyStatus.AVAILABLE),
                    property(sky, "Sky Residences - 1508", PropertyType.FLAT, "1508", "Hinjewadi", "Pune",
                            "1650", "13200000", 3, 3, PropertyStatus.AVAILABLE),
                    property(biz, "Business Hub - Office 302", PropertyType.OFFICE_SPACE, "302", "Gachibowli", "Hyderabad",
                            "2200", "26400000", null, 2, PropertyStatus.AVAILABLE),
                    property(null, "Whitefield Plot 45", PropertyType.PLOT, "P45", "Whitefield", "Bengaluru",
                            "1800", "7200000", null, null, PropertyStatus.AVAILABLE)));

            // ---- Sample leads across the workflow ----
            seedLead(leadRepo, activityRepo, callRepo, visitRepo, adminUser,
                    "Ramesh Kumar", "9812345678", "ramesh@example.com", SourceChannel.FACEBOOK,
                    "Diwali Facebook Campaign", PropertyType.FLAT, "4000000", "6000000", "Hinjewadi, Pune",
                    LeadPriority.HOT, caller1, LeadStatus.INTERESTED);

            seedLead(leadRepo, activityRepo, callRepo, visitRepo, adminUser,
                    "Sunita Iyer", "9822233344", "sunita@example.com", SourceChannel.GOOGLE,
                    "Google Search - Villas Bengaluru", PropertyType.VILLA, "15000000", "25000000", "Sarjapur Road",
                    LeadPriority.WARM, caller1, LeadStatus.SITE_VISIT_SCHEDULED);

            seedLead(leadRepo, activityRepo, callRepo, visitRepo, adminUser,
                    "Mohan Das", "9833344455", "mohan@example.com", SourceChannel.NEWSPAPER,
                    "Times of India Ad", PropertyType.OFFICE_SPACE, "20000000", "30000000", "Gachibowli",
                    LeadPriority.WARM, caller2, LeadStatus.CALLING);

            seedLead(leadRepo, activityRepo, callRepo, visitRepo, adminUser,
                    "Aisha Khan", "9844455566", "aisha@example.com", SourceChannel.INSTAGRAM,
                    "Instagram Reel", PropertyType.FLAT, "8000000", "12000000", "Hinjewadi",
                    LeadPriority.COLD, caller2, LeadStatus.NOT_INTERESTED);

            seedLead(leadRepo, activityRepo, callRepo, visitRepo, adminUser,
                    "Vikram Singh", "9855566677", "vikram@example.com", SourceChannel.REFERRAL,
                    "Referral - existing customer", PropertyType.VILLA, "18000000", "24000000", "Sarjapur Road",
                    LeadPriority.HOT, sales1, LeadStatus.NEGOTIATION);

            for (int i = 1; i <= 12; i++) {
                seedLead(leadRepo, activityRepo, callRepo, visitRepo, adminUser,
                        "Prospect " + i, "90000000" + String.format("%02d", i), "prospect" + i + "@example.com",
                        SourceChannel.values()[i % SourceChannel.values().length],
                        "Campaign " + i, PropertyType.values()[i % 4],
                        "3000000", "9000000", i % 2 == 0 ? "Pune" : "Bengaluru",
                        LeadPriority.WARM, i % 2 == 0 ? caller1 : caller2,
                        i % 3 == 0 ? LeadStatus.NEW : LeadStatus.ASSIGNED);
            }

            log.info("Seed complete. Users: {} | Leads: {} | Login password for all: {}",
                    userRepo.count(), leadRepo.count(), DEFAULT_PASSWORD);
        };
    }

    private Team team(TeamRepository repo, String name, String desc) {
        Team t = new Team();
        t.setName(name);
        t.setDescription(desc);
        return repo.save(t);
    }

    private User user(UserRepository repo, PasswordEncoder encoder, String fullName, String username,
                      String email, String phone, Team team, Set<Role> roles) {
        User u = new User();
        u.setFullName(fullName);
        u.setUsername(username);
        u.setEmail(email);
        u.setPhone(phone);
        u.setTeam(team);
        u.setRoles(roles);
        u.setActive(true);
        u.setPasswordHash(encoder.encode(DEFAULT_PASSWORD));
        return repo.save(u);
    }

    private PropertyProject project(String name, String location, String city, String desc) {
        PropertyProject p = new PropertyProject();
        p.setName(name);
        p.setLocation(location);
        p.setCity(city);
        p.setDescription(desc);
        return p;
    }

    private Property property(PropertyProject project, String title, PropertyType type, String unit,
                             String location, String city, String area, String price,
                             Integer beds, Integer baths, PropertyStatus status) {
        Property p = new Property();
        p.setProject(project);
        p.setTitle(title);
        p.setPropertyType(type);
        p.setUnitNumber(unit);
        p.setLocation(location);
        p.setCity(city);
        p.setAreaSqft(new BigDecimal(area));
        p.setPrice(new BigDecimal(price));
        p.setBedrooms(beds);
        p.setBathrooms(baths);
        p.setStatus(status);
        return p;
    }

    private void seedLead(LeadRepository leadRepo, LeadActivityRepository activityRepo,
                          CallLogRepository callRepo, SiteVisitRepository visitRepo, User creator,
                          String name, String mobile, String email, SourceChannel channel, String source,
                          PropertyType type, String budgetMin, String budgetMax, String location,
                          LeadPriority priority, User assignee, LeadStatus status) {
        if (leadRepo.existsByMobileAndDeletedFalse(mobile)) return;

        Lead lead = new Lead();
        lead.setCustomerName(name);
        lead.setMobile(mobile);
        lead.setEmail(email);
        lead.setSourceChannel(channel);
        lead.setSource(source);
        lead.setPropertyType(type);
        lead.setBudgetMin(new BigDecimal(budgetMin));
        lead.setBudgetMax(new BigDecimal(budgetMax));
        lead.setPreferredLocation(location);
        lead.setPriority(priority);
        lead.setStatus(status);
        lead.setCreatedByUser(creator);
        if (status != LeadStatus.NEW) {
            lead.setAssignedUser(assignee);
            lead.setNextFollowUpAt(Instant.now().plus(1, ChronoUnit.DAYS));
        }
        leadRepo.save(lead);

        activity(activityRepo, lead, ActivityType.CREATED, null, LeadStatus.NEW, "Lead created via " + channel, creator);
        if (status != LeadStatus.NEW) {
            activity(activityRepo, lead, ActivityType.ASSIGNED, LeadStatus.NEW, LeadStatus.ASSIGNED,
                    "Assigned to " + assignee.getFullName(), creator);
        }

        if (EnumSet.of(LeadStatus.CALLING, LeadStatus.INTERESTED, LeadStatus.NOT_INTERESTED,
                LeadStatus.SITE_VISIT_SCHEDULED, LeadStatus.NEGOTIATION).contains(status)) {
            CallLog call = new CallLog();
            call.setLead(lead);
            call.setCaller(assignee);
            call.setDirection(CallDirection.OUTBOUND);
            call.setOutcome(status == LeadStatus.NOT_INTERESTED ? CallOutcome.CONNECTED : CallOutcome.CONNECTED);
            call.setDisposition(switch (status) {
                case INTERESTED, SITE_VISIT_SCHEDULED, NEGOTIATION -> CallDisposition.INTERESTED;
                case NOT_INTERESTED -> CallDisposition.NOT_INTERESTED;
                default -> CallDisposition.FOLLOW_UP;
            });
            call.setDurationSeconds(120 + (int) (Math.random() * 180));
            call.setNotes("Discussed requirement and budget.");
            call.setCalledAt(Instant.now().minus(1, ChronoUnit.DAYS));
            callRepo.save(call);
            activity(activityRepo, lead, ActivityType.CALL, status, status, "Call: CONNECTED", assignee);
        }

        if (EnumSet.of(LeadStatus.SITE_VISIT_SCHEDULED, LeadStatus.NEGOTIATION).contains(status)) {
            SiteVisit visit = new SiteVisit();
            visit.setLead(lead);
            visit.setSalesExecutive(assignee.getRoles().stream().anyMatch(r -> r.getName() == RoleName.SALES_EXECUTIVE)
                    ? assignee : null);
            visit.setScheduledAt(Instant.now().plus(2, ChronoUnit.DAYS));
            visit.setStatus(status == LeadStatus.NEGOTIATION ? SiteVisitStatus.COMPLETED : SiteVisitStatus.SCHEDULED);
            if (status == LeadStatus.NEGOTIATION) {
                visit.setResult(SiteVisitResult.READY_TO_BOOK);
                visit.setCompletedAt(Instant.now().minus(1, ChronoUnit.HOURS));
                visit.setFeedback("Very positive, ready to discuss pricing.");
            }
            visitRepo.save(visit);
            activity(activityRepo, lead, ActivityType.SITE_VISIT_SCHEDULED, status, status,
                    "Site visit " + visit.getStatus(), assignee);
        }
    }

    private void activity(LeadActivityRepository repo, Lead lead, ActivityType type,
                          LeadStatus from, LeadStatus to, String summary, User actor) {
        LeadActivity a = new LeadActivity();
        a.setLead(lead);
        a.setType(type);
        a.setFromStatus(from);
        a.setToStatus(to);
        a.setSummary(summary);
        a.setActor(actor);
        a.setOccurredAt(Instant.now());
        repo.save(a);
    }
}
