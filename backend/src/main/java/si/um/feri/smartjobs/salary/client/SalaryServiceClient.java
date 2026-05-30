package si.um.feri.smartjobs.salary.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import si.um.feri.smartjobs.job.dto.JobFilterRequest;
import si.um.feri.smartjobs.salary.config.SalaryProperties;
import si.um.feri.smartjobs.salary.dto.SalaryPredictionResponse;

@Component
public class SalaryServiceClient {

    private final RestTemplate restTemplate;
    private final SalaryProperties salaryProperties;

    public SalaryServiceClient(RestTemplate restTemplate, SalaryProperties salaryProperties) {
        this.restTemplate = restTemplate;
        this.salaryProperties = salaryProperties;
    }

    public SalaryPredictionResponse predict(JobFilterRequest request) {
        SalaryPredictionResponse response = restTemplate.postForObject(
                salaryProperties.serviceUrl() + "/predict",
                request,
                SalaryPredictionResponse.class
        );

        if (response == null) {
            throw new IllegalStateException("Salary service did not return a prediction response.");
        }

        return response;
    }
}
