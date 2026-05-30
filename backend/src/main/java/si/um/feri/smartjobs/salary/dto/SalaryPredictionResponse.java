package si.um.feri.smartjobs.salary.dto;

import java.math.BigDecimal;

public record SalaryPredictionResponse(
        Boolean available,
        BigDecimal predictedMinSalary,
        BigDecimal predictedMaxSalary,
        String currency,
        Integer profileCompleteness,
        Integer similarJobs,
        String market,
        Boolean marketAssumed,
        String message,
        BigDecimal modelMae
) {
}
