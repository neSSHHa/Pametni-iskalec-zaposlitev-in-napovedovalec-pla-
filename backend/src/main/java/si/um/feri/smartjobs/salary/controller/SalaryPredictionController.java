package si.um.feri.smartjobs.salary.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import si.um.feri.smartjobs.job.dto.JobFilterRequest;
import si.um.feri.smartjobs.salary.client.SalaryServiceClient;
import si.um.feri.smartjobs.salary.dto.SalaryPredictionResponse;

@RestController
@RequestMapping("/api/salary")
public class SalaryPredictionController {

    private final SalaryServiceClient salaryServiceClient;

    public SalaryPredictionController(SalaryServiceClient salaryServiceClient) {
        this.salaryServiceClient = salaryServiceClient;
    }

    @PostMapping("/predict")
    public SalaryPredictionResponse predict(@RequestBody JobFilterRequest request) {
        return salaryServiceClient.predict(request);
    }
}
