package si.um.feri.smartjobs.analytics.dto;

import java.util.List;

public record AnalyticsDashboardDto(
        AnalyticsSummaryDto summary,
        List<CountStatDto> topSkills,
        List<CountStatDto> topRoles,
        List<LocationStatDto> cityStats,
        List<LocationStatDto> regionStats,
        List<LocationStatDto> countryStats,
        List<CountStatDto> experienceLevelStats,
        List<CountStatDto> workTypeStats,
        List<CountStatDto> educationLevelStats,
        List<CountStatDto> sourceStats,
        SalaryStatsDto salaryStats
) {
}
