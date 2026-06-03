package si.um.feri.smartjobs.salary.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import si.um.feri.smartjobs.job.dto.JobFilterRequest;
import si.um.feri.smartjobs.salary.client.SalaryServiceClient;
import si.um.feri.smartjobs.salary.dto.SalaryPredictionResponse;

@RestController
@RequestMapping("/api/salary")
@Tag(name = "Salary", description = "Salary prediction endpoints that estimate market salary ranges from job-related criteria.")
public class SalaryPredictionController {

    private final SalaryServiceClient salaryServiceClient;

    public SalaryPredictionController(SalaryServiceClient salaryServiceClient) {
        this.salaryServiceClient = salaryServiceClient;
    }

    @PostMapping("/predict")
    @Operation(
            summary = "Predict salary range",
            description = "Sends job criteria to the salary service and returns an estimated minimum and maximum salary range with confidence-related metadata."
    )
    @ApiResponse(responseCode = "200", description = "Salary prediction was returned successfully.")
    public SalaryPredictionResponse predict(@RequestBody JobFilterRequest request) {
        return salaryServiceClient.predict(request);
    }
}
