package si.um.feri.smartjobs.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "High-level analytics summary for the job database.")
public record AnalyticsSummaryDto(
        @Schema(description = "Total number of job postings.", example = "12500")
        long totalJobs,
        @Schema(description = "Total number of distinct companies.", example = "940")
        long totalCompanies,
        @Schema(description = "Total number of distinct locations.", example = "180")
        long totalLocations,
        @Schema(description = "Total number of countries represented in the dataset.", example = "3")
        long totalCountries,
        @Schema(description = "Number of jobs with salary information.", example = "4200")
        long jobsWithSalary,
        @Schema(description = "Number of remote jobs.", example = "860")
        long remoteJobs,
        @Schema(description = "Average salary estimate across jobs with salary data.", example = "3150")
        BigDecimal averageSalary,
        @Schema(description = "Highest salary found in the dataset.", example = "9000")
        BigDecimal highestSalary
) {
}
