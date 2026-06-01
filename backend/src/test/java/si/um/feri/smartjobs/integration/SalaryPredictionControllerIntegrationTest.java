package si.um.feri.smartjobs.integration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import si.um.feri.smartjobs.job.dto.JobFilterRequest;
import si.um.feri.smartjobs.salary.client.SalaryServiceClient;
import si.um.feri.smartjobs.salary.dto.SalaryPredictionResponse;

class SalaryPredictionControllerIntegrationTest extends AbstractIntegrationTestData {

    @MockBean
    private SalaryServiceClient salaryServiceClient;

    @Test
    void shouldReturnSalaryPredictionFromBackendEndpoint() throws Exception {
        when(salaryServiceClient.predict(any(JobFilterRequest.class)))
                .thenReturn(new SalaryPredictionResponse(
                        true,
                        BigDecimal.valueOf(2600),
                        BigDecimal.valueOf(3400),
                        "EUR",
                        90,
                        21217,
                        "Austria",
                        false,
                        "Prediction is based on Austrian salary data.",
                        BigDecimal.valueOf(425.02)
                ));

        mockMvc.perform(post("/api/salary/predict")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(austriaJavaRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available", is(true)))
                .andExpect(jsonPath("$.predictedMinSalary", is(2600)))
                .andExpect(jsonPath("$.predictedMaxSalary", is(3400)))
                .andExpect(jsonPath("$.currency", is("EUR")))
                .andExpect(jsonPath("$.market", is("Austria")))
                .andExpect(jsonPath("$.profileCompleteness", is(90)));
    }

    @Test
    void shouldReturnUnavailableSalaryPredictionWhenSalaryServiceCannotPredict() throws Exception {
        when(salaryServiceClient.predict(any(JobFilterRequest.class)))
                .thenReturn(new SalaryPredictionResponse(
                        false,
                        null,
                        null,
                        "EUR",
                        0,
                        0,
                        "Austria",
                        false,
                        "Salary prediction is currently available for Austria-based searches.",
                        null
                ));

        mockMvc.perform(post("/api/salary/predict")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sloveniaRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available", is(false)))
                .andExpect(jsonPath("$.predictedMinSalary").doesNotExist())
                .andExpect(jsonPath("$.predictedMaxSalary").doesNotExist())
                .andExpect(jsonPath("$.message", is("Salary prediction is currently available for Austria-based searches.")));
    }

    private JobFilterRequest austriaJavaRequest() {
        return new JobFilterRequest(
                new JobFilterRequest.JobCriteria(
                        null,
                        "Java Developer",
                        null,
                        2,
                        null,
                        null,
                        null,
                        LocalDate.of(2026, 5, 20),
                        null,
                        null,
                        "Junior",
                        "Bachelor"
                ),
                new JobFilterRequest.LocationCriteria(
                        null,
                        "Vienna",
                        List.of("Vienna"),
                        null,
                        List.of(),
                        "Austria",
                        List.of("Austria"),
                        null,
                        null
                ),
                List.of("Hybrid"),
                List.of("Java", "SQL")
        );
    }

    private JobFilterRequest sloveniaRequest() {
        return new JobFilterRequest(
                new JobFilterRequest.JobCriteria(
                        null,
                        "Java Developer",
                        null,
                        2,
                        null,
                        null,
                        null,
                        LocalDate.of(2026, 5, 20),
                        null,
                        null,
                        "Junior",
                        "Bachelor"
                ),
                new JobFilterRequest.LocationCriteria(
                        null,
                        "Ljubljana",
                        List.of("Ljubljana"),
                        null,
                        List.of(),
                        "Slovenia",
                        List.of("Slovenia"),
                        null,
                        null
                ),
                List.of("Hybrid"),
                List.of("Java", "SQL")
        );
    }
}