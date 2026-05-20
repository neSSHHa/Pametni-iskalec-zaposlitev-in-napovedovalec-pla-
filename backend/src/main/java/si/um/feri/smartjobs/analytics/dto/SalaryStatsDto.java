package si.um.feri.smartjobs.analytics.dto;

import java.math.BigDecimal;

public record SalaryStatsDto(
        long jobsWithSalary,
        BigDecimal lowestSalary,
        BigDecimal highestSalary,
        BigDecimal averageMinSalary,
        BigDecimal averageMaxSalary,
        BigDecimal averageSalary
) {
}
