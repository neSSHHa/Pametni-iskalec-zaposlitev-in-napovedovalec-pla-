package si.um.feri.smartjobs.integration;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import si.um.feri.smartjobs.ai.dto.NaturalLanguageJobFilterRequest;

@EnabledIfEnvironmentVariable(named = "RUN_LIVE_AI_TESTS", matches = "true")
class AiLivePreDeploymentIntegrationTest extends AbstractIntegrationTestData {

    @Test
    void shouldReachLiveAiService() throws Exception {
        mockMvc.perform(post("/api/ai/jobs/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NaturalLanguageJobFilterRequest("jobs in Austria", "thinking"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiResponse", notNullValue()));
    }

    @Test
    void shouldReturnValidJsonShapeFromLivePrompt() throws Exception {
        mockMvc.perform(post("/api/ai/jobs/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NaturalLanguageJobFilterRequest("remote React jobs in Austria", "thinking"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiResponse.skills").isArray())
                .andExpect(jsonPath("$.aiResponse.workTypes").isArray())
                .andExpect(jsonPath("$.filterRequest", notNullValue()));
    }

    @Test
    void shouldExtractCountryFromLivePrompt() throws Exception {
        mockMvc.perform(post("/api/ai/jobs/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NaturalLanguageJobFilterRequest("jobs in Austria", "thinking"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filterRequest.location.country", is("Austria")));
    }

    @Test
    void shouldExtractCityFromLivePrompt() throws Exception {
        mockMvc.perform(post("/api/ai/jobs/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NaturalLanguageJobFilterRequest("jobs in Wien Austria", "thinking"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filterRequest.location.city", is("Vienna")));
    }

    @Test
    void shouldExtractWorkTypeFromLivePrompt() throws Exception {
        mockMvc.perform(post("/api/ai/jobs/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NaturalLanguageJobFilterRequest("remote jobs in Austria", "thinking"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filterRequest.workTypes[0]", is("Remote")));
    }

    @Test
    void shouldExtractSkillFromLivePrompt() throws Exception {
        mockMvc.perform(post("/api/ai/jobs/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NaturalLanguageJobFilterRequest("React jobs in Austria", "thinking"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filterRequest.skills[0]", is("React")));
    }

    @Test
    void shouldExtractMultipleFiltersFromLivePrompt() throws Exception {
        mockMvc.perform(post("/api/ai/jobs/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NaturalLanguageJobFilterRequest("remote React jobs in Wien, Austria", "thinking"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filterRequest.location.city", is("Vienna")))
                .andExpect(jsonPath("$.filterRequest.location.country", is("Austria")))
                .andExpect(jsonPath("$.filterRequest.workTypes[0]", is("Remote")))
                .andExpect(jsonPath("$.filterRequest.skills[0]", is("React")));
    }

    @Test
    void shouldSearchAustriaJobsUsingLiveAiFilter() throws Exception {
        mockMvc.perform(post("/api/ai/jobs/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NaturalLanguageJobFilterRequest("jobs in Austria", "thinking"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.jobs[0].country", is("Austria")));
    }

    @Test
    void shouldSearchWienJobsUsingLiveAiFilter() throws Exception {
        mockMvc.perform(post("/api/ai/jobs/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NaturalLanguageJobFilterRequest("jobs in Wien Austria", "thinking"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.jobs[0].city", is("Vienna")));
    }

    @Test
    void shouldSearchSkillJobsUsingLiveAiFilter() throws Exception {
        mockMvc.perform(post("/api/ai/jobs/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NaturalLanguageJobFilterRequest("React jobs", "thinking"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.jobs[0].title", containsString("React")));
    }

    @Test
    void shouldHandleUnknownSkillWithoutBreakingLiveSearch() throws Exception {
        mockMvc.perform(post("/api/ai/jobs/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NaturalLanguageJobFilterRequest("quantum banana engineer jobs in Austria", "thinking"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs").isArray());
    }

    @Test
    void shouldHandleVagueLivePromptWithoutServerError() throws Exception {
        mockMvc.perform(post("/api/ai/jobs/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NaturalLanguageJobFilterRequest("something interesting", "thinking"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs").isArray());
    }

    @Test
    void shouldRewriteCvToProfileTextWithLiveAi() throws Exception {
        mockMvc.perform(multipart("/api/cv/rewrite-profile").file(textFile(
                        "cv.txt",
                        "I am a React developer with Java experience in Vienna."
                )))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("I")));
    }

    @Test
    void shouldExtractCvFilterFromSimpleLiveCv() throws Exception {
        mockMvc.perform(multipart("/api/cv/extract-filter").file(textFile(
                        "cv.txt",
                        "I am a React developer with Java experience in Vienna and I prefer remote jobs in Austria."
                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filterRequest.skills").isArray())
                .andExpect(jsonPath("$.filterRequest.location.country", is("Austria")));
    }

    @Test
    void shouldReturnMatchedJobsFromLiveCvText() throws Exception {
        mockMvc.perform(multipart("/api/cv/jobs/filter")
                        .file(textFile("cv.txt", "I am a React developer with Java experience in Vienna."))
                        .param("mode", "thinking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs").isArray());
    }

    @Test
    void shouldHandleCvWithNoClearSkillsWithoutServerError() throws Exception {
        mockMvc.perform(multipart("/api/cv/jobs/filter")
                        .file(textFile("cv.txt", "I am reliable and open to work."))
                        .param("mode", "thinking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs").isArray());
    }

    private MockMultipartFile textFile(String filename, String text) {
        return new MockMultipartFile("file", filename, "text/plain", text.getBytes(StandardCharsets.UTF_8));
    }
}
