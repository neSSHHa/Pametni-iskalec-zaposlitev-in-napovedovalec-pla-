package si.um.feri.smartjobs.analytics.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import si.um.feri.smartjobs.analytics.dto.AnalyticsDashboardDto;
import si.um.feri.smartjobs.analytics.dto.AnalyticsSummaryDto;
import si.um.feri.smartjobs.analytics.dto.CountStatDto;
import si.um.feri.smartjobs.analytics.dto.LocationStatDto;
import si.um.feri.smartjobs.analytics.dto.SalaryStatsDto;
import si.um.feri.smartjobs.analytics.service.AnalyticsService;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics", description = "Aggregated job market statistics for dashboards, charts, and reporting views.")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping
    @Operation(summary = "Get analytics dashboard", description = "Returns the full analytics dashboard payload with summary metrics, top skills, top roles, location statistics, category distributions, and salary statistics.")
    @ApiResponse(responseCode = "200", description = "Analytics dashboard was returned successfully.")
    public AnalyticsDashboardDto getAnalytics(@RequestParam(defaultValue = "10") int limit) {
        return analyticsService.dashboard(limit);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get analytics dashboard", description = "Returns the same dashboard payload as GET /api/analytics.")
    @ApiResponse(responseCode = "200", description = "Analytics dashboard was returned successfully.")
    public AnalyticsDashboardDto getDashboard(@RequestParam(defaultValue = "10") int limit) {
        return analyticsService.dashboard(limit);
    }

    @GetMapping("/summary")
    @Operation(summary = "Get analytics summary", description = "Returns high-level platform totals such as jobs, companies, locations, countries, salary coverage, remote jobs, and salary averages.")
    @ApiResponse(responseCode = "200", description = "Analytics summary was returned successfully.")
    public AnalyticsSummaryDto getSummary() {
        return analyticsService.summary();
    }

    @GetMapping("/skills")
    @Operation(summary = "Get top skills", description = "Returns the most common skills found in job postings.")
    @ApiResponse(responseCode = "200", description = "Skill statistics were returned successfully.")
    public List<CountStatDto> getSkillStats(@RequestParam(defaultValue = "10") int limit) {
        return analyticsService.topSkills(limit);
    }

    @GetMapping("/roles")
    @Operation(summary = "Get top roles", description = "Returns the most common job roles or titles in the job database.")
    @ApiResponse(responseCode = "200", description = "Role statistics were returned successfully.")
    public List<CountStatDto> getRoleStats(@RequestParam(defaultValue = "10") int limit) {
        return analyticsService.topRoles(limit);
    }

    @GetMapping("/locations")
    public List<LocationStatDto> getLocationStats(
            @Parameter(description = "Location aggregation level: city, region, or country.", example = "city")
            @RequestParam(defaultValue = "city") String level,
            @Parameter(description = "Maximum number of location rows to return.", example = "20")
            @RequestParam(defaultValue = "20") int limit
    ) {
        return analyticsService.locationStats(level, limit);
    }

    @GetMapping("/experience-levels")
    @Operation(summary = "Get experience level distribution", description = "Returns the distribution of jobs by experience level.")
    public List<CountStatDto> getExperienceLevelStats(@RequestParam(defaultValue = "10") int limit) {
        return analyticsService.experienceLevelStats(limit);
    }

    @GetMapping("/work-types")
    @Operation(summary = "Get work type distribution", description = "Returns the distribution of jobs by work type or work mode.")
    public List<CountStatDto> getWorkTypeStats(@RequestParam(defaultValue = "10") int limit) {
        return analyticsService.workTypeStats(limit);
    }

    @GetMapping("/education-levels")
    @Operation(summary = "Get education level distribution", description = "Returns the distribution of jobs by required or preferred education level.")
    public List<CountStatDto> getEducationLevelStats(@RequestParam(defaultValue = "10") int limit) {
        return analyticsService.educationLevelStats(limit);
    }

    @GetMapping("/sources")
    @Operation(summary = "Get source distribution", description = "Returns the distribution of jobs by source website or import source.")
    public List<CountStatDto> getSourceStats(@RequestParam(defaultValue = "10") int limit) {
        return analyticsService.sourceStats(limit);
    }

    @GetMapping("/salary")
    @Operation(summary = "Get salary statistics", description = "Returns salary coverage, minimum and maximum salary values, and average salary estimates.")
    @ApiResponse(responseCode = "200", description = "Salary statistics were returned successfully.")
    public SalaryStatsDto getSalaryStats() {
        return analyticsService.salaryStats();
    }
}
