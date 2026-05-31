package si.um.feri.smartjobs.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import si.um.feri.smartjobs.ai.client.AiServiceClient;
import si.um.feri.smartjobs.ai.dto.AiJobFilterExtractionResponse;
import si.um.feri.smartjobs.ai.dto.NaturalLanguageJobFilterRequest;

class AiSearchIntegrationTest extends AbstractIntegrationTestData {

    @MockBean
    private AiServiceClient aiServiceClient;

    @Test
    void shouldExtractFiltersFromPrompt() throws Exception {
        when(aiServiceClient.extractJobFilter(eq("remote jobs in Austria"), anyList(), anyList(), anyList(), anyList(), anyList()))
                .thenReturn(austriaRemoteResponse());

        mockMvc.perform(post("/api/ai/jobs/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NaturalLanguageJobFilterRequest("remote jobs in Austria", "thinking"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filterRequest.location.country", is("Austria")))
                .andExpect(jsonPath("$.filterRequest.workTypes[0]", is("Remote")));
    }

    @Test
    void shouldSearchAustriaJobsFromPrompt() throws Exception {
        when(aiServiceClient.extractJobFilter(eq("jobs in Austria"), anyList(), anyList(), anyList(), anyList(), anyList()))
                .thenReturn(countryResponse("Austria"));

        mockMvc.perform(post("/api/ai/jobs/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NaturalLanguageJobFilterRequest("jobs in Austria", "thinking"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs[0].country", is("Austria")));
    }

    @Test
    void shouldSearchWienRemoteJobsFromPrompt() throws Exception {
        when(aiServiceClient.extractJobFilter(eq("remote jobs in Wien"), anyList(), anyList(), anyList(), anyList(), anyList()))
                .thenReturn(viennaRemoteResponse());

        mockMvc.perform(post("/api/ai/jobs/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NaturalLanguageJobFilterRequest("remote jobs in Wien", "thinking"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs[0].city", is("Vienna")))
                .andExpect(jsonPath("$.jobs[0].workMode", is("Remote, Hybrid")));
    }

    @Test
    void shouldSearchJobsFromSkillPrompt() throws Exception {
        when(aiServiceClient.extractJobFilter(eq("React jobs"), anyList(), anyList(), anyList(), anyList(), anyList()))
                .thenReturn(skillResponse("React"));

        mockMvc.perform(post("/api/ai/jobs/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NaturalLanguageJobFilterRequest("React jobs", "thinking"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs[0].title", is("React Developer")));
    }

    @Test
    void shouldReturnJobsWhenAiReturnsValidFilter() throws Exception {
        when(aiServiceClient.extractJobFilter(eq("Mechatronics in Graz"), anyList(), anyList(), anyList(), anyList(), anyList()))
                .thenReturn(new AiJobFilterExtractionResponse(
                        null,
                        new AiJobFilterExtractionResponse.LocationData(null, "Graz", List.of("Graz"), null, List.of(), "Austria", List.of("Austria"), null, null),
                        List.of("Field work"),
                        List.of("Mechatronics"),
                        List.of()
                ));

        mockMvc.perform(post("/api/ai/jobs/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NaturalLanguageJobFilterRequest("Mechatronics in Graz", "thinking"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs[0].title", is("Mechatronics Technician")));
    }

    @Test
    void shouldFallbackToFastParserWhenAiUnavailable() throws Exception {
        when(aiServiceClient.extractJobFilter(eq("Mechatronics jobs in Austria"), anyList(), anyList(), anyList(), anyList(), anyList()))
                .thenThrow(new IllegalStateException("AI unavailable"));

        mockMvc.perform(post("/api/ai/jobs/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NaturalLanguageJobFilterRequest("Mechatronics jobs in Austria", "thinking"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs[0].title", is("Mechatronics Technician")));
    }

    @Test
    void shouldFallbackToFastParserWhenAiReturnsInvalidResponse() throws Exception {
        when(aiServiceClient.extractJobFilter(eq("remote jobs in Austria"), anyList(), anyList(), anyList(), anyList(), anyList()))
                .thenReturn(new AiJobFilterExtractionResponse(null, null, List.of(), List.of(), List.of()));

        mockMvc.perform(post("/api/ai/jobs/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NaturalLanguageJobFilterRequest("remote jobs in Austria", "thinking"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs[0].workMode", is("Remote, Hybrid")));
    }

    @Test
    void shouldUseFastModeWhenModeIsNotThinking() throws Exception {
        mockMvc.perform(post("/api/ai/jobs/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NaturalLanguageJobFilterRequest("React jobs in Wien", "fast"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs[0].city", is("Vienna")));

        verify(aiServiceClient, never()).extractJobFilter(eq("React jobs in Wien"), anyList(), anyList(), anyList(), anyList(), anyList());
    }

    @Test
    void shouldUseThinkingModeWhenRequested() throws Exception {
        when(aiServiceClient.extractJobFilter(eq("jobs in Austria"), anyList(), anyList(), anyList(), anyList(), anyList()))
                .thenReturn(countryResponse("Austria"));

        mockMvc.perform(post("/api/ai/jobs/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NaturalLanguageJobFilterRequest("jobs in Austria", "thinking"))))
                .andExpect(status().isOk());

        verify(aiServiceClient).extractJobFilter(eq("jobs in Austria"), anyList(), anyList(), anyList(), anyList(), anyList());
    }

    @Test
    void shouldHandleEmptyPromptAccordingToCurrentApiBehavior() throws Exception {
        mockMvc.perform(post("/api/ai/jobs/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NaturalLanguageJobFilterRequest("", "fast"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs", hasSize(0)));
    }

    private AiJobFilterExtractionResponse countryResponse(String country) {
        return new AiJobFilterExtractionResponse(
                null,
                new AiJobFilterExtractionResponse.LocationData(null, null, List.of(), null, List.of(), country, List.of(country), null, null),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private AiJobFilterExtractionResponse skillResponse(String skill) {
        return new AiJobFilterExtractionResponse(null, null, List.of(), List.of(skill), List.of());
    }

    private AiJobFilterExtractionResponse austriaRemoteResponse() {
        return new AiJobFilterExtractionResponse(
                null,
                new AiJobFilterExtractionResponse.LocationData(null, null, List.of(), null, List.of(), "Austria", List.of("Austria"), null, null),
                List.of("Remote"),
                List.of(),
                List.of()
        );
    }

    private AiJobFilterExtractionResponse viennaRemoteResponse() {
        return new AiJobFilterExtractionResponse(
                null,
                new AiJobFilterExtractionResponse.LocationData(null, "Vienna", List.of("Vienna"), null, List.of(), "Austria", List.of("Austria"), null, null),
                List.of("Remote"),
                List.of(),
                List.of()
        );
    }
}
