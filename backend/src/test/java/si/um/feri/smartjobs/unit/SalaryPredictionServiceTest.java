package si.um.feri.smartjobs.unit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import si.um.feri.smartjobs.job.dto.JobFilterRequest;
import si.um.feri.smartjobs.salary.client.SalaryServiceClient;
import si.um.feri.smartjobs.salary.config.SalaryProperties;
import si.um.feri.smartjobs.salary.controller.SalaryPredictionController;
import si.um.feri.smartjobs.salary.dto.SalaryPredictionResponse;

@ExtendWith(MockitoExtension.class)
class SalaryPredictionServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private SalaryServiceClient salaryServiceClient;

    @Test
    void shouldCallSalaryServicePredictEndpoint() {
        SalaryServiceClient client = new SalaryServiceClient(
                restTemplate,
                new SalaryProperties("http://salary-service:8091")
        );
        JobFilterRequest request = austriaJavaRequest();
        SalaryPredictionResponse expectedResponse = successfulSalaryResponse();

        when(restTemplate.postForObject(
                eq("http://salary-service:8091/predict"),
                eq(request),
                eq(SalaryPredictionResponse.class)
        )).thenReturn(expectedResponse);

        SalaryPredictionResponse actualResponse = client.predict(request);

        assertSame(expectedResponse, actualResponse);
        verify(restTemplate).postForObject(
                "http://salary-service:8091/predict",
                request,
                SalaryPredictionResponse.class
        );
    }

    @Test
    void shouldSendOriginalFilterRequestToSalaryService() {
        SalaryServiceClient client = new SalaryServiceClient(
                restTemplate,
                new SalaryProperties("http://salary-service:8091")
        );
        JobFilterRequest request = austriaJavaRequest();

        when(restTemplate.postForObject(
                eq("http://salary-service:8091/predict"),
                eq(request),
                eq(SalaryPredictionResponse.class)
        )).thenReturn(successfulSalaryResponse());

        client.predict(request);

        ArgumentCaptor<JobFilterRequest> requestCaptor = ArgumentCaptor.forClass(JobFilterRequest.class);
        verify(restTemplate).postForObject(
                eq("http://salary-service:8091/predict"),
                requestCaptor.capture(),
                eq(SalaryPredictionResponse.class)
        );

        JobFilterRequest sentRequest = requestCaptor.getValue();

        assertEquals("Java Developer", sentRequest.job().jobname());
        assertEquals(2, sentRequest.job().requiredExperience());
        assertEquals("Junior", sentRequest.job().experienceLevelName());
        assertEquals("Bachelor", sentRequest.job().educationLevel());
        assertEquals("Vienna", sentRequest.location().city());
        assertEquals("Austria", sentRequest.location().country());
        assertEquals(List.of("Java", "SQL"), sentRequest.skills());
        assertEquals(List.of("Hybrid"), sentRequest.workTypes());
    }

    @Test
    void shouldThrowWhenSalaryServiceReturnsNullResponse() {
        SalaryServiceClient client = new SalaryServiceClient(
                restTemplate,
                new SalaryProperties("http://salary-service:8091")
        );
        JobFilterRequest request = austriaJavaRequest();

        when(restTemplate.postForObject(
                eq("http://salary-service:8091/predict"),
                eq(request),
                eq(SalaryPredictionResponse.class)
        )).thenReturn(null);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> client.predict(request)
        );

        assertEquals("Salary service did not return a prediction response.", exception.getMessage());
    }

    @Test
    void shouldPropagateExceptionWhenSalaryServiceIsUnavailable() {
        SalaryServiceClient client = new SalaryServiceClient(
                restTemplate,
                new SalaryProperties("http://salary-service:8091")
        );
        JobFilterRequest request = austriaJavaRequest();

        when(restTemplate.postForObject(
                eq("http://salary-service:8091/predict"),
                eq(request),
                eq(SalaryPredictionResponse.class)
        )).thenThrow(new RestClientException("Connection refused"));

        assertThrows(RestClientException.class, () -> client.predict(request));
    }

    @Test
    void shouldDelegateControllerRequestToSalaryServiceClient() {
        SalaryPredictionController controller = new SalaryPredictionController(salaryServiceClient);
        JobFilterRequest request = austriaJavaRequest();
        SalaryPredictionResponse expectedResponse = successfulSalaryResponse();

        when(salaryServiceClient.predict(request)).thenReturn(expectedResponse);

        SalaryPredictionResponse actualResponse = controller.predict(request);

        assertSame(expectedResponse, actualResponse);
        verify(salaryServiceClient).predict(request);
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

    private SalaryPredictionResponse successfulSalaryResponse() {
        return new SalaryPredictionResponse(
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
        );
    }
}