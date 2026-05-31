package si.um.feri.smartjobs.integration;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;

import si.um.feri.smartjobs.ai.client.AiServiceClient;
import si.um.feri.smartjobs.ai.dto.AiJobFilterExtractionResponse;

class CvMatchingIntegrationTest extends AbstractIntegrationTestData {

    @MockBean
    private AiServiceClient aiServiceClient;

    @Test
    void shouldExtractTextFromUploadedCv() throws Exception {
        MockMultipartFile file = textFile("cv.txt", "I am a React developer in Vienna.");

        mockMvc.perform(multipart("/api/cv/extract-text").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename", is("cv.txt")))
                .andExpect(jsonPath("$.contentType", is("text/plain")))
                .andExpect(jsonPath("$.text", containsString("React developer")));
    }

    @Test
    void shouldReturnMatchedJobsForCvUpload() throws Exception {
        MockMultipartFile file = textFile("cv.txt", "Experienced React developer with Java and frontend work.");

        mockMvc.perform(multipart("/api/cv/jobs/filter").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename", is("cv.txt")))
                .andExpect(jsonPath("$.jobs[0].title", is("React Developer")));
    }

    @Test
    void shouldUseFastCvModeByDefault() throws Exception {
        MockMultipartFile file = textFile("cv.txt", "React frontend developer.");

        mockMvc.perform(multipart("/api/cv/jobs/filter").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filterRequest.skills[0]", is("React")));
    }

    @Test
    void shouldReturnFilterUsedForCvMatching() throws Exception {
        MockMultipartFile file = textFile("cv.txt", "Mechatronics technician with robotics experience.");

        mockMvc.perform(multipart("/api/cv/jobs/filter").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filterRequest.skills[0]", is("Mechatronics")))
                .andExpect(jsonPath("$.jobs[0].title", is("Mechatronics Technician")));
    }

    @Test
    void shouldReturnNoJobsWhenCvHasNoExtractedCriteria() throws Exception {
        MockMultipartFile file = textFile("cv.txt", "Reliable candidate with broad interests.");

        mockMvc.perform(multipart("/api/cv/jobs/filter").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filterRequest.skills", hasSize(0)))
                .andExpect(jsonPath("$.jobs", hasSize(0)));
    }

    @Test
    void shouldUseThinkingCvModeWhenRequested() throws Exception {
        when(aiServiceClient.extractCvJobFilter(eq("Nurse with patient care experience."), anyList(), anyList(), anyList(), anyList(), anyList()))
                .thenReturn(new AiJobFilterExtractionResponse(null, null, List.of("On-site"), List.of("Nursing"), List.of()));
        MockMultipartFile file = textFile("cv.txt", "Nurse with patient care experience.");

        mockMvc.perform(multipart("/api/cv/jobs/filter").file(file).param("mode", "thinking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filterRequest.skills[0]", is("Nursing")))
                .andExpect(jsonPath("$.jobs[0].title", is("Nurse")));
    }

    @Test
    void shouldReturnMatchedJobsWhenStubAiExtractsCvFilter() throws Exception {
        when(aiServiceClient.extractCvJobFilter(eq("I work in hotel service in Vienna."), anyList(), anyList(), anyList(), anyList(), anyList()))
                .thenReturn(new AiJobFilterExtractionResponse(
                        null,
                        new AiJobFilterExtractionResponse.LocationData(null, "Vienna", List.of("Vienna"), null, List.of(), "Austria", List.of("Austria"), null, null),
                        List.of("On-site"),
                        List.of("Hotel Service"),
                        List.of()
                ));
        MockMultipartFile file = textFile("cv.txt", "I work in hotel service in Vienna.");

        mockMvc.perform(multipart("/api/cv/jobs/filter").file(file).param("mode", "thinking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs[0].title", is("Hotel Service Associate")));
    }

    @Test
    void shouldRewriteCvToProfileTextUsingStubAi() throws Exception {
        when(aiServiceClient.rewriteCvToProfileText(eq("React developer CV.")))
                .thenReturn("I am a React developer looking for frontend jobs.");
        MockMultipartFile file = textFile("cv.txt", "React developer CV.");

        mockMvc.perform(multipart("/api/cv/rewrite-profile").file(file))
                .andExpect(status().isOk())
                .andExpect(content().string("I am a React developer looking for frontend jobs."));
    }

    private MockMultipartFile textFile(String filename, String text) {
        return new MockMultipartFile("file", filename, "text/plain", text.getBytes(StandardCharsets.UTF_8));
    }
}
