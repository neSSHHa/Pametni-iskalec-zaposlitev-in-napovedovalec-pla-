package si.um.feri.smartjobs.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Aggregated salary statistics for jobs with salary information.")
public record SalaryStatsDto(
        @Schema(description = "Number of jobs that include salary information.", example = "4200")
        long jobsWithSalary,
        @Schema(description = "Lowest salary value in the dataset.", example = "1200")
        BigDecimal lowestSalary,
        @Schema(description = "Highest salary value in the dataset.", example = "9000")
        BigDecimal highestSalary,
        @Schema(description = "Average minimum salary.", example = "2600")
        BigDecimal averageMinSalary,
        @Schema(description = "Average maximum salary.", example = "4100")
        BigDecimal averageMaxSalary,
        @Schema(description = "Average salary estimate.", example = "3350")
        BigDecimal averageSalary
) {
}
