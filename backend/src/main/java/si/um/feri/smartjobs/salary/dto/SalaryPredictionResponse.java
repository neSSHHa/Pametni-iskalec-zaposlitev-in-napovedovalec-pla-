package si.um.feri.smartjobs.salary.dto;

import java.math.BigDecimal;

public record SalaryPredictionResponse(
        Boolean available,
        BigDecimal predictedMinSalary,
        BigDecimal predictedMaxSalary,
        String currency,
        Integer confidence,
        Integer similarJobs,
        String market,
        String message,
        BigDecimal modelMae
) {
}
