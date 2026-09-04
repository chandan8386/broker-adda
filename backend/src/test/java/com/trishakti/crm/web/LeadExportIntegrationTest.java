package com.trishakti.crm.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trishakti.crm.domain.User;
import com.trishakti.crm.domain.enums.RoleName;
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
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The CSV export streams outside the request transaction and reads lead.assignedUser, which is a
 * lazy proxy. With open-in-view disabled that previously threw LazyInitializationException (HTTP 500).
 * It must also honour role scoping, so a calling-team user cannot export leads they cannot see.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LeadExportIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String adminToken;

    /**
     * Creates its own admin directly rather than going through /auth/register-first-admin: the
     * in-memory database is shared across test classes, so whether an admin already exists depends
     * on execution order.
     */
    @BeforeEach
    void bootstrap() throws Exception {
        if (userRepository.findByUsernameIgnoreCase("exportadmin").isEmpty()) {
            User admin = new User();
            admin.setFullName("Export Admin");
            admin.setUsername("exportadmin");
            admin.setEmail("exportadmin@trishakti.com");
            admin.setPasswordHash(passwordEncoder.encode("Password@123"));
            admin.setActive(true);
            admin.setRoles(Set.of(roleRepository.findByName(RoleName.ADMIN).orElseThrow()));
            userRepository.save(admin);
        }
        adminToken = token(mockMvc.perform(post("/auth/login").contentType("application/json")
                        .content("""
                                {"usernameOrEmail":"exportadmin","password":"Password@123"}"""))
                .andExpect(status().isOk()).andReturn());
    }

    private String token(MvcResult r) throws Exception {
        JsonNode n = objectMapper.readTree(r.getResponse().getContentAsString());
        return n.get("accessToken").asText();
    }

    private String exportCsv(String bearer, String query) throws Exception {
        return mockMvc.perform(get("/leads/export" + query).header("Authorization", "Bearer " + bearer))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    void exportsAssignedLeadWithoutLazyInitializationError() throws Exception {
        long adminId = objectMapper.readTree(
                mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + adminToken))
                        .andReturn().getResponse().getContentAsString()).get("id").asLong();

        // a lead WITH an assignee is what triggers the lazy proxy on export
        mockMvc.perform(post("/leads").header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("""
                                {"customerName":"Export Target","mobile":"9700000001",
                                 "sourceChannel":"FACEBOOK","propertyType":"FLAT","assignedUserId":%d}"""
                                .formatted(adminId)))
                .andExpect(status().isCreated());

        String csv = exportCsv(adminToken, "");

        assertThat(csv).startsWith("customerName,mobile,email");
        assertThat(csv).contains("reference,status,assignedUser,createdAt");
        assertThat(csv).contains("Export Target");
        assertThat(csv).contains("Export Admin"); // the resolved assignee name, not a proxy blow-up
    }

    @Test
    void exportHonoursStatusFilter() throws Exception {
        mockMvc.perform(post("/leads").header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("""
                                {"customerName":"Filter Probe","mobile":"9700000002","propertyType":"VILLA"}"""))
                .andExpect(status().isCreated());

        assertThat(exportCsv(adminToken, "?status=NEW")).contains("Filter Probe");
        // PURCHASED matches nothing -> header only, no data rows
        String purchased = exportCsv(adminToken, "?status=PURCHASED");
        assertThat(purchased).doesNotContain("Filter Probe");
        assertThat(purchased.strip().lines()).hasSize(1);
    }

    @Test
    void exportIsScopedToWhatTheUserMaySee() throws Exception {
        mockMvc.perform(post("/leads").header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("""
                                {"customerName":"Admin Only Lead","mobile":"9700000003","propertyType":"PLOT"}"""))
                .andExpect(status().isCreated());

        if (userRepository.findByUsernameIgnoreCase("caller3").isEmpty()) {
            User caller = new User();
            caller.setFullName("Caller Three");
            caller.setUsername("caller3");
            caller.setEmail("caller3@trishakti.com");
            caller.setPasswordHash(passwordEncoder.encode("Password@123"));
            caller.setActive(true);
            caller.setRoles(Set.of(roleRepository.findByName(RoleName.CALLING_TEAM).orElseThrow()));
            userRepository.save(caller);
        }

        String callerToken = token(mockMvc.perform(post("/auth/login").contentType("application/json")
                        .content("""
                                {"usernameOrEmail":"caller3","password":"Password@123"}"""))
                .andExpect(status().isOk()).andReturn());

        assertThat(exportCsv(adminToken, "")).contains("Admin Only Lead");
        // the calling-team user owns none of these leads, so the export must not leak them
        assertThat(exportCsv(callerToken, "")).doesNotContain("Admin Only Lead");
    }
}
