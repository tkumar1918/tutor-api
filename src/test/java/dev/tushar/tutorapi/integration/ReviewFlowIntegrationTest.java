package dev.tushar.tutorapi.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

/**
 * End-to-end review lifecycle: eligibility gating (must have an ACCEPTED request), one review
 * per student–tutor pair, public listing, edit/delete, and the rating aggregate surfacing on
 * the public tutor catalog.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReviewFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;

    @Test
    void review_requires_accepted_request_is_unique_and_drives_catalog_rating() throws Exception {
        long tutorProfileId = approvedTutor("tess", "Tess", "Tutor");

        // A student with no engagement yet can't review — 422
        register("quentin", "quentin@example.com", "Quentin", "Quick");
        String quentinToken = login("quentin", "Secret123!");
        mockMvc.perform(post("/api/v1/tutors/" + tutorProfileId + "/reviews")
                        .header("Authorization", "Bearer " + quentinToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"comment\":\"too soon\"}"))
                .andExpect(status().isUnprocessableContent());

        // Student sends a request; tutor accepts it
        String tutorToken = login("tess", "Secret123!");
        long requestId = sendRequest(quentinToken, tutorProfileId);
        mockMvc.perform(patch("/api/v1/tutoring-requests/" + requestId)
                        .header("Authorization", "Bearer " + tutorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isOk());

        // Now the review is allowed — 201
        MvcResult reviewResult = mockMvc.perform(post("/api/v1/tutors/" + tutorProfileId + "/reviews")
                        .header("Authorization", "Bearer " + quentinToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"comment\":\"Brilliant tutor\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.studentName").value("Quentin Quick"))
                .andReturn();
        long reviewId = objectMapper.readTree(reviewResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        assertThat(reviewId).isGreaterThan(0);

        // Reviewing the same tutor twice → 409
        mockMvc.perform(post("/api/v1/tutors/" + tutorProfileId + "/reviews")
                        .header("Authorization", "Bearer " + quentinToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":4,\"comment\":\"again\"}"))
                .andExpect(status().isConflict());

        // Public (no auth) listing shows the review
        mockMvc.perform(get("/api/v1/tutors/" + tutorProfileId + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].rating").value(5))
                .andExpect(jsonPath("$.data.content[0].studentName").value("Quentin Quick"));

        // Public catalog detail now carries the aggregate
        mockMvc.perform(get("/api/v1/tutors/" + tutorProfileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.averageRating").value(5.0))
                .andExpect(jsonPath("$.data.reviewCount").value(1));

        // Author edits the rating → aggregate follows
        mockMvc.perform(patch("/api/v1/reviews/" + reviewId)
                        .header("Authorization", "Bearer " + quentinToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":3,\"comment\":\"revised\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rating").value(3));
        mockMvc.perform(get("/api/v1/tutors/" + tutorProfileId))
                .andExpect(jsonPath("$.data.averageRating").value(3.0))
                .andExpect(jsonPath("$.data.reviewCount").value(1));

        // A stranger can't edit someone else's review → 403
        register("mallory", "mallory@example.com", "Mal", "Lory");
        String malloryToken = login("mallory", "Secret123!");
        mockMvc.perform(patch("/api/v1/reviews/" + reviewId)
                        .header("Authorization", "Bearer " + malloryToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":1,\"comment\":\"hijack\"}"))
                .andExpect(status().isForbidden());

        // Author deletes it → catalog drops back to no rating
        mockMvc.perform(delete("/api/v1/reviews/" + reviewId)
                        .header("Authorization", "Bearer " + quentinToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/tutors/" + tutorProfileId))
                .andExpect(jsonPath("$.data.averageRating").doesNotExist())
                .andExpect(jsonPath("$.data.reviewCount").value(0));
    }

    @Test
    void cannot_review_yourself() throws Exception {
        long tutorProfileId = approvedTutor("solo", "So", "Lo");
        String token = login("solo", "Secret123!");
        mockMvc.perform(post("/api/v1/tutors/" + tutorProfileId + "/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"comment\":\"I'm great\"}"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void rating_out_of_range_is_rejected() throws Exception {
        long tutorProfileId = approvedTutor("ruth", "Ruth", "Range");
        register("validator", "validator@example.com", "Val", "Idator");
        String token = login("validator", "Secret123!");
        mockMvc.perform(post("/api/v1/tutors/" + tutorProfileId + "/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":6,\"comment\":\"over\"}"))
                .andExpect(status().isBadRequest());
    }

    /** Register a user, apply, admin-approve, and return the tutor profile id. */
    private long approvedTutor(String username, String first, String last) throws Exception {
        register(username, username + "@example.com", first, last);
        String token = login(username, "Secret123!");
        mockMvc.perform(post("/api/v1/me/tutor-application")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bio":"x","expertise":"MATH","qualifications":"MSc","yearsOfExperience":4,"hourlyRateCents":5000}
                                """))
                .andExpect(status().isCreated());

        String adminName = username + "Admin";
        register(adminName, adminName + "@example.com", "Adm", "In");
        User admin = userRepository.findByUsername(adminName).orElseThrow();
        admin.setAdmin(true);
        userRepository.save(admin);
        String adminToken = login(adminName, "Secret123!");

        MvcResult appResult = mockMvc.perform(get("/api/v1/me/tutor-application")
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        long appId = objectMapper.readTree(appResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(post("/api/v1/admin/tutor-applications/" + appId + "/review")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk());
        return appId; // TutorProfile.id == application id (same row)
    }

    private long sendRequest(String studentToken, long tutorProfileId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/tutoring-requests")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tutorId":%d,"subject":"MATH","message":"Need help"}
                                """.formatted(tutorProfileId)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
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
