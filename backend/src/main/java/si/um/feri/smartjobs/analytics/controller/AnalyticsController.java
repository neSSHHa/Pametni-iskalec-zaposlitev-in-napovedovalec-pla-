package si.um.feri.smartjobs.analytics.controller;

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
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping
    public AnalyticsDashboardDto getAnalytics(@RequestParam(defaultValue = "10") int limit) {
        return analyticsService.dashboard(limit);
    }

    @GetMapping("/dashboard")
    public AnalyticsDashboardDto getDashboard(@RequestParam(defaultValue = "10") int limit) {
        return analyticsService.dashboard(limit);
    }

    @GetMapping("/summary")
    public AnalyticsSummaryDto getSummary() {
        return analyticsService.summary();
    }

    @GetMapping("/skills")
    public List<CountStatDto> getSkillStats(@RequestParam(defaultValue = "10") int limit) {
        return analyticsService.topSkills(limit);
    }

    @GetMapping("/roles")
    public List<CountStatDto> getRoleStats(@RequestParam(defaultValue = "10") int limit) {
        return analyticsService.topRoles(limit);
    }

    @GetMapping("/locations")
    public List<LocationStatDto> getLocationStats(
            @RequestParam(defaultValue = "city") String level,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return analyticsService.locationStats(level, limit);
    }

    @GetMapping("/experience-levels")
    public List<CountStatDto> getExperienceLevelStats(@RequestParam(defaultValue = "10") int limit) {
        return analyticsService.experienceLevelStats(limit);
    }

    @GetMapping("/work-types")
    public List<CountStatDto> getWorkTypeStats(@RequestParam(defaultValue = "10") int limit) {
        return analyticsService.workTypeStats(limit);
    }

    @GetMapping("/education-levels")
    public List<CountStatDto> getEducationLevelStats(@RequestParam(defaultValue = "10") int limit) {
        return analyticsService.educationLevelStats(limit);
    }

    @GetMapping("/sources")
    public List<CountStatDto> getSourceStats(@RequestParam(defaultValue = "10") int limit) {
        return analyticsService.sourceStats(limit);
    }

    @GetMapping("/salary")
    public SalaryStatsDto getSalaryStats() {
        return analyticsService.salaryStats();
    }
}
