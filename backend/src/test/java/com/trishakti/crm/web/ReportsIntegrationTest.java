package com.trishakti.crm.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trishakti.crm.domain.Lead;
import com.trishakti.crm.domain.User;
import com.trishakti.crm.domain.enums.LeadStatus;
import com.trishakti.crm.domain.enums.RoleName;
import com.trishakti.crm.repository.LeadRepository;
import com.trishakti.crm.repository.RoleRepository;
import com.trishakti.crm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The performance reports used to be driven from Lead/CallLog and grouped by whoever happened to
 * be attached, which meant calling-team members showed up under "Sales team performance" and any
 * user with no rows vanished from their own report. Both reports must now be driven from User,
 * filtered by role, and must include people with zero activity.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportsIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired LeadRepository leadRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String adminToken;

    private User ensureUser(String username, String fullName, RoleName role) {
        return userRepository.findByUsernameIgnoreCase(username).orElseGet(() -> {
            User u = new User();
            u.setFullName(fullName);
            u.setUsername(username);
            u.setEmail(username + "@trishakti.com");
            u.setPasswordHash(passwordEncoder.encode("Password@123"));
            u.setActive(true);
            u.setRoles(Set.of(roleRepository.findByName(role).orElseThrow()));
            return userRepository.save(u);
        });
    }

    @BeforeEach
    void setUp() throws Exception {
        ensureUser("rptadmin", "Report Admin", RoleName.ADMIN);
        var res = mockMvc.perform(post("/auth/login").contentType("application/json")
                        .content("""
                                {"usernameOrEmail":"rptadmin","password":"Password@123"}"""))
                .andExpect(status().isOk()).andReturn();
        adminToken = objectMapper.readTree(res.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private JsonNode reports() throws Exception {
        return objectMapper.readTree(
                mockMvc.perform(get("/dashboard/reports").header("Authorization", "Bearer " + adminToken))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());
    }

    private List<String> names(JsonNode arr) {
        return arr.findValuesAsText("name");
    }

    @Test
    void salesReportExcludesCallingTeamAndIncludesExecutivesWithNoLeads() throws Exception {
        User caller = ensureUser("rptcaller", "Report Caller", RoleName.CALLING_TEAM);
        ensureUser("rptexec", "Report Exec", RoleName.SALES_EXECUTIVE);          // zero leads
        User busyExec = ensureUser("rptexec2", "Busy Exec", RoleName.SALES_EXECUTIVE);

        // give the CALLING_TEAM member a lead - previously this pulled them into the sales report
        Lead a = new Lead();
        a.setCustomerName("Caller Lead");
        a.setMobile("9600000001");
        a.setStatus(LeadStatus.ASSIGNED);
        a.setAssignedUser(caller);
        leadRepository.save(a);

        Lead b = new Lead();
        b.setCustomerName("Exec Lead");
        b.setMobile("9600000002");
        b.setStatus(LeadStatus.INTERESTED);
        b.setAssignedUser(busyExec);
        leadRepository.save(b);

        List<String> sales = names(reports().get("salesTeam"));

        assertThat(sales).contains("Busy Exec");
        assertThat(sales).as("sales executive with no leads must still be listed").contains("Report Exec");
        assertThat(sales).as("calling team must not appear in the sales report").doesNotContain("Report Caller");
    }

    @Test
    void callingReportListsCallingTeamIncludingAgentsWithNoCalls() throws Exception {
        ensureUser("rptcaller3", "Quiet Caller", RoleName.CALLING_TEAM);   // never made a call
        ensureUser("rptexec3", "Sales Only", RoleName.SALES_EXECUTIVE);

        List<String> calling = names(reports().get("callingTeam"));

        assertThat(calling).as("agent with zero calls must still be listed").contains("Quiet Caller");
        assertThat(calling).as("sales executives do not belong in the calling report").doesNotContain("Sales Only");
    }

    @Test
    void zeroActivityRowsReportZeroNotNull() throws Exception {
        ensureUser("rptexec4", "Idle Exec", RoleName.SALES_EXECUTIVE);

        JsonNode row = null;
        for (JsonNode n : reports().get("salesTeam")) {
            if ("Idle Exec".equals(n.get("name").asText())) row = n;
        }
        assertThat(row).as("idle executive present in report").isNotNull();
        assertThat(row.get("assignedLeads").asLong()).isZero();
        assertThat(row.get("interested").asLong()).isZero();
        assertThat(row.get("purchased").asLong()).isZero();
        assertThat(row.get("closeRate").asDouble()).isZero();
    }
}
