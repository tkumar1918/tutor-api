package dev.tushar.tutorapi.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.tushar.tutorapi.dto.request.LoginRequest;
import dev.tushar.tutorapi.dto.request.RegisterRequest;
import dev.tushar.tutorapi.entity.User;
import dev.tushar.tutorapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;

    @Test
    void apply_twice_returns_409() throws Exception {
        register("dup", "dup@example.com", "Dup", "Licant");
        String token = login("dup", "Secret123!");
        String body = """
                {"bio":"...","expertise":"MATH","qualifications":"BSc Math","yearsOfExperience":1,"hourlyRateCents":5000}
                """;
        mockMvc.perform(post("/api/v1/me/tutor-application")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/me/tutor-application")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void admin_endpoints_blocked_for_regular_user() throws Exception {
        register("regular", "regular@example.com", "Reg", "Ular");
        String token = login("regular", "Secret123!");
        mockMvc.perform(get("/api/v1/admin/tutor-applications")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticated_me_returns_401() throws Exception {
        mockMvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void approval_invalidates_applicants_old_jwt() throws Exception {
        // 1) Eve registers, applies, gets a working JWT (tv=0)
        register("eve", "eve@example.com", "Eve", "Doe");
        String oldEveToken = login("eve", "Secret123!");

        mockMvc.perform(post("/api/v1/me/tutor-application")
                        .header("Authorization", "Bearer " + oldEveToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bio":"Math expert","expertise":"MATH","qualifications":"PhD Math","yearsOfExperience":12,"hourlyRateCents":5000}
                                """))
                .andExpect(status().isCreated());

        // Sanity: the old token works before approval
        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + oldEveToken))
                .andExpect(status().isOk());

        // 2) Provision an admin and approve Eve's application
        register("mod", "mod@example.com", "Mod", "Erator");
        User admin = userRepository.findByUsername("mod").orElseThrow();
        admin.setAdmin(true);
        userRepository.save(admin);
        String modToken = login("mod", "Secret123!");

        MvcResult mineResult = mockMvc.perform(get("/api/v1/me/tutor-application")
                        .header("Authorization", "Bearer " + oldEveToken))
                .andExpect(status().isOk())
                .andReturn();
        long appId = objectMapper
                .readTree(mineResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(post("/api/v1/admin/tutor-applications/" + appId + "/review")
                        .header("Authorization", "Bearer " + modToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk());

        // 3) Eve's OLD token is now stale — server rejects it as unauthenticated
        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + oldEveToken))
                .andExpect(status().isUnauthorized());

        // 4) Re-login mints a fresh JWT carrying the bumped tv — that works
        String newEveToken = login("eve", "Secret123!");
        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + newEveToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authorities").value(
                        org.hamcrest.Matchers.hasItem("ROLE_TUTOR")));
    }

    @Test
    void student_sends_tutoring_request_tutor_accepts() throws Exception {
        // 1) Tutor registers, applies, gets approved, re-logs in
        register("riley", "riley@example.com", "Riley", "Smith");
        String rileyToken = login("riley", "Secret123!");
        mockMvc.perform(post("/api/v1/me/tutor-application")
                        .header("Authorization", "Bearer " + rileyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bio":"Math","expertise":"MATH","qualifications":"MSc Math","yearsOfExperience":5,"hourlyRateCents":6000}
                                """))
                .andExpect(status().isCreated());

        register("rootR", "rootR@example.com", "Root", "Reviewer");
        User admin = userRepository.findByUsername("rootR").orElseThrow();
        admin.setAdmin(true);
        userRepository.save(admin);
        String adminToken = login("rootR", "Secret123!");

        MvcResult appResult = mockMvc.perform(get("/api/v1/me/tutor-application")
                        .header("Authorization", "Bearer " + rileyToken))
                .andReturn();
        long appId = objectMapper.readTree(appResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        long tutorProfileId = appId; // TutorProfile.id == application id (same row)

        mockMvc.perform(post("/api/v1/admin/tutor-applications/" + appId + "/review")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk());

        rileyToken = login("riley", "Secret123!"); // refresh — tv bumped

        // 2) A student registers and sends a request
        register("sam", "sam@example.com", "Sam", "Student");
        String samToken = login("sam", "Secret123!");

        MvcResult createResult = mockMvc.perform(post("/api/v1/tutoring-requests")
                        .header("Authorization", "Bearer " + samToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tutorId":%d,"subject":"MATH","message":"Need calc help"}
                                """.formatted(tutorProfileId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();
        long requestId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 3) Student sees it in /mine
        mockMvc.perform(get("/api/v1/tutoring-requests/mine")
                        .header("Authorization", "Bearer " + samToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value((int) requestId));

        // 4) Tutor sees it in /incoming
        mockMvc.perform(get("/api/v1/tutoring-requests/incoming")
                        .header("Authorization", "Bearer " + rileyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value((int) requestId));

        // 5) A different user can't view a request that isn't theirs
        register("nosy", "nosy@example.com", "Nosy", "Neighbor");
        String nosyToken = login("nosy", "Secret123!");
        mockMvc.perform(get("/api/v1/tutoring-requests/" + requestId)
                        .header("Authorization", "Bearer " + nosyToken))
                .andExpect(status().isForbidden());

        // 6) Student can't accept their own request (only the tutor can)
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/v1/tutoring-requests/" + requestId)
                        .header("Authorization", "Bearer " + samToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isForbidden());

        // 7) Tutor accepts
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/v1/tutoring-requests/" + requestId)
                        .header("Authorization", "Bearer " + rileyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACCEPTED\",\"tutorReply\":\"Looking forward to it!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.tutorReply").value("Looking forward to it!"));

        // 8) Tutor can't accept again — terminal state
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/v1/tutoring-requests/" + requestId)
                        .header("Authorization", "Bearer " + rileyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\"}"))
                .andExpect(status().isUnprocessableContent());

        // 9) Student can't cancel an already-ACCEPTED request
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/tutoring-requests/" + requestId)
                        .header("Authorization", "Bearer " + samToken))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void notifications_count_pending_items_per_role() throws Exception {
        // Plain user → both counts are 0
        register("noah", "noah@example.com", "Noah", "User");
        String noahToken = login("noah", "Secret123!");
        mockMvc.perform(get("/api/v1/me/notifications")
                        .header("Authorization", "Bearer " + noahToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tutorPendingRequests").value(0))
                .andExpect(jsonPath("$.data.adminPendingApplications").value(0));

        // Admin sees pending applications: provision admin, register an applicant
        register("notifAdmin", "notifAdmin@example.com", "Notif", "Admin");
        User admin = userRepository.findByUsername("notifAdmin").orElseThrow();
        admin.setAdmin(true);
        userRepository.save(admin);
        String adminToken = login("notifAdmin", "Secret123!");

        register("paula", "paula@example.com", "Paula", "Pending");
        String paulaToken = login("paula", "Secret123!");
        mockMvc.perform(post("/api/v1/me/tutor-application")
                        .header("Authorization", "Bearer " + paulaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bio":"x","expertise":"MATH","qualifications":"BSc","yearsOfExperience":2,"hourlyRateCents":3000}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/me/notifications")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.adminPendingApplications").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        // Approved tutor with an incoming PENDING request → tutorPendingRequests >= 1
        long appId = objectMapper.readTree(mockMvc.perform(get("/api/v1/me/tutor-application")
                        .header("Authorization", "Bearer " + paulaToken))
                .andReturn().getResponse().getContentAsString())
                .path("data").path("id").asLong();
        mockMvc.perform(post("/api/v1/admin/tutor-applications/" + appId + "/review")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk());
        paulaToken = login("paula", "Secret123!"); // refresh after tv bump

        register("quinn", "quinn@example.com", "Quinn", "Student");
        String quinnToken = login("quinn", "Secret123!");
        mockMvc.perform(post("/api/v1/tutoring-requests")
                        .header("Authorization", "Bearer " + quinnToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tutorId":%d,"subject":"MATH","message":"please help"}
                                """.formatted(appId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/me/notifications")
                        .header("Authorization", "Bearer " + paulaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tutorPendingRequests").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.adminPendingApplications").value(0));
    }

    @Test
    void cannot_send_tutoring_request_to_yourself() throws Exception {
        register("selfy", "selfy@example.com", "Sel", "Fy");
        String selfyToken = login("selfy", "Secret123!");
        mockMvc.perform(post("/api/v1/me/tutor-application")
                        .header("Authorization", "Bearer " + selfyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bio":"Self","expertise":"MATH","qualifications":"BSc Math","yearsOfExperience":3,"hourlyRateCents":4000}
                                """))
                .andExpect(status().isCreated());

        register("rootS", "rootS@example.com", "Root", "S");
        User admin = userRepository.findByUsername("rootS").orElseThrow();
        admin.setAdmin(true);
        userRepository.save(admin);
        String adminToken = login("rootS", "Secret123!");

        MvcResult appResult = mockMvc.perform(get("/api/v1/me/tutor-application")
                        .header("Authorization", "Bearer " + selfyToken))
                .andReturn();
        long appId = objectMapper.readTree(appResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(post("/api/v1/admin/tutor-applications/" + appId + "/review")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk());

        selfyToken = login("selfy", "Secret123!");

        mockMvc.perform(post("/api/v1/tutoring-requests")
                        .header("Authorization", "Bearer " + selfyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tutorId":%d,"subject":"MATH","message":"Hi me"}
                                """.formatted(appId)))
                .andExpect(status().isUnprocessableContent());
    }

    private void register(String username, String email, String first, String last) throws Exception {
        RegisterRequest req = new RegisterRequest(username, email, "Secret123!", first, last, null);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    private String login(String username, String password) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(r.getResponse().getContentAsString());
        String token = body.path("data").path("token").asString();
        assertThat(token).isNotBlank();
        return token;
    }
}
