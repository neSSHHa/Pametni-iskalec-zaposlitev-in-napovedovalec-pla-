package si.um.feri.smartjobs.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import si.um.feri.smartjobs.job.dto.JobFilterRequest;
import si.um.feri.smartjobs.job.dto.TextSearchRequest;

class JobControllerIntegrationTest extends AbstractIntegrationTestData {

    @Test
    void shouldReturnJobs() throws Exception {
        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs", hasSize(4)))
                .andExpect(jsonPath("$.jobs[0].id", notNullValue()));
    }

    @Test
    void shouldReturnPaginatedResponse() throws Exception {
        mockMvc.perform(get("/api/jobs").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs", hasSize(2)))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(2)))
                .andExpect(jsonPath("$.hasMore", is(true)));
    }

    @Test
    void shouldReturnTotalCount() throws Exception {
        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount", is(4)));
    }

    @Test
    void shouldFilterJobsByCountry() throws Exception {
        JobFilterRequest request = new JobFilterRequest(
                null,
                new JobFilterRequest.LocationCriteria(null, null, List.of(), null, List.of(), "Austria", List.of("Austria"), null, null),
                List.of(),
                List.of()
        );

        mockMvc.perform(post("/api/jobs/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs[0].country", is("Austria")));
    }

    @Test
    void shouldFilterJobsByCity() throws Exception {
        JobFilterRequest request = new JobFilterRequest(
                null,
                new JobFilterRequest.LocationCriteria(null, "Vienna", List.of("Vienna"), null, List.of(), "Austria", List.of("Austria"), null, null),
                List.of(),
                List.of()
        );

        mockMvc.perform(post("/api/jobs/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs[0].city", is("Vienna")));
    }

    @Test
    void shouldFilterJobsBySkill() throws Exception {
        JobFilterRequest request = new JobFilterRequest(null, null, List.of(), List.of("React"));

        mockMvc.perform(post("/api/jobs/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs[0].skills[0]", is("React")));
    }

    @Test
    void shouldFilterJobsByWorkType() throws Exception {
        JobFilterRequest request = new JobFilterRequest(null, null, List.of("Remote"), List.of());

        mockMvc.perform(post("/api/jobs/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs[0].workMode", is("Remote, Hybrid")));
    }

    @Test
    void shouldSearchJobsByText() throws Exception {
        mockMvc.perform(post("/api/jobs/text-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TextSearchRequest("React"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title", is("React Developer")));
    }

    @Test
    void shouldReturnEmptyResultsForNoMatch() throws Exception {
        mockMvc.perform(post("/api/jobs/text-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TextSearchRequest("zzzz-no-match"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
