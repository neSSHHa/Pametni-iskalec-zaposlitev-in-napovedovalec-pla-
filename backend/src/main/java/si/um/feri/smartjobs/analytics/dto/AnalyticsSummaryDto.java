package si.um.feri.smartjobs.analytics.dto;

import java.math.BigDecimal;

public record AnalyticsSummaryDto(
        long totalJobs,
        long totalCompanies,
        long totalLocations,
        long totalCountries,
        long jobsWithSalary,
        long remoteJobs,
        BigDecimal averageSalary,
        BigDecimal highestSalary
) {
}
