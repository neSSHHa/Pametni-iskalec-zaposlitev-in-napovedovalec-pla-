package si.um.feri.smartjobs.integration;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class AnalyticsControllerIntegrationTest extends AbstractIntegrationTestData {

    @Test
    void shouldReturnAnalyticsSummary() throws Exception {
        mockMvc.perform(get("/api/analytics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalJobs", is(4)))
                .andExpect(jsonPath("$.totalCompanies", is(4)));
    }

    @Test
    void shouldCountTotalJobs() throws Exception {
        mockMvc.perform(get("/api/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalJobs", is(4)));
    }

    @Test
    void shouldCountAustriaJobs() throws Exception {
        mockMvc.perform(get("/api/analytics/locations").param("level", "country"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.label == 'Austria')].count").value(hasItem(3)));
    }

    @Test
    void shouldReturnTopLocations() throws Exception {
        mockMvc.perform(get("/api/analytics/locations").param("level", "city").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].label", is("Vienna")))
                .andExpect(jsonPath("$[0].count", is(2)));
    }

    @Test
    void shouldIncludeWienInLocationStats() throws Exception {
        mockMvc.perform(get("/api/analytics/locations").param("level", "city"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.label == 'Vienna')].country").value(hasItem("Austria")));
    }

    @Test
    void shouldReturnTopSkills() throws Exception {
        mockMvc.perform(get("/api/analytics/skills"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].label").value(hasItem("React")));
    }

    @Test
    void shouldReturnExperienceDistribution() throws Exception {
        mockMvc.perform(get("/api/analytics/experience-levels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].label").value(hasItem("Junior")))
                .andExpect(jsonPath("$[*].label").value(hasItem("Senior")));
    }

    @Test
    void shouldReturnSalaryStats() throws Exception {
        mockMvc.perform(get("/api/analytics/salary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobsWithSalary", is(3)))
                .andExpect(jsonPath("$.highestSalary", is(4500.0)));
    }

    @Test
    void shouldHandleNoSalaryData() throws Exception {
        workTypeJobRepository.deleteAll();
        jobSkillRepository.deleteAll();
        jobRepository.deleteAll();
        jobRepository.save(new si.um.feri.smartjobs.job.entity.Job(
                "job-no-salary-only",
                "No Salary Company",
                "No Salary Role",
                "No salary data.",
                null,
                null,
                null,
                "integration-test",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 1),
                null,
                null,
                junior,
                vienna,
                bachelor
        ));

        mockMvc.perform(get("/api/analytics/salary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobsWithSalary", is(0)))
                .andExpect(jsonPath("$.lowestSalary", nullValue()))
                .andExpect(jsonPath("$.highestSalary", nullValue()));
    }

    @Test
    void shouldHandleEmptyDatabase() throws Exception {
        clearData();

        mockMvc.perform(get("/api/analytics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalJobs", is(0)))
                .andExpect(jsonPath("$.totalCompanies", is(0)))
                .andExpect(jsonPath("$.totalLocations", is(0)));
    }

    @Test
    void shouldReturnRemoteJobsCount() throws Exception {
        mockMvc.perform(get("/api/analytics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remoteJobs", greaterThan(0)));
    }
}
