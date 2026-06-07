package si.um.feri.smartjobs.admin.controller;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import si.um.feri.smartjobs.ai.service.AiAllowedValuesService;
import si.um.feri.smartjobs.analytics.service.AnalyticsService;
import si.um.feri.smartjobs.admin.dto.AdminCacheComponentDto;
import si.um.feri.smartjobs.admin.dto.AdminCacheStatusDto;
import si.um.feri.smartjobs.job.service.JobService;

@RestController
@RequestMapping("/api/admin/cache")
@Tag(name = "Admin", description = "Operational maintenance endpoints for refreshing in-memory caches and search indexes.")
public class AdminCacheController {

    private final AiAllowedValuesService aiAllowedValuesService;
    private final AnalyticsService analyticsService;
    private final JobService jobService;

    public AdminCacheController(
            AiAllowedValuesService aiAllowedValuesService,
            AnalyticsService analyticsService,
            JobService jobService
    ) {
        this.aiAllowedValuesService = aiAllowedValuesService;
        this.analyticsService = analyticsService;
        this.jobService = jobService;
    }

    @org.springframework.web.bind.annotation.GetMapping("/status")
    @Operation(
            summary = "Backend cache status",
            description = "Returns whether operational in-memory caches have been loaded and how many entries they contain."
    )
    public AdminCacheStatusDto cacheStatus() {
        List<AdminCacheComponentDto> components = List.of(
                new AdminCacheComponentDto(
                        "AI allowed values",
                        aiAllowedValuesService.isCacheLoaded(),
                        aiAllowedValuesService.cacheSizes()
                ),
                new AdminCacheComponentDto(
                        "Job lookup index",
                        jobService.isJobLookupIndexLoaded(),
                        jobService.cacheSizes()
                ),
                new AdminCacheComponentDto(
                        "Skill relation index",
                        jobService.isSkillRelationIndexLoaded(),
                        Map.of("skillRelationKeys", jobService.cacheSizes().getOrDefault("skillRelationKeys", 0))
                ),
                new AdminCacheComponentDto(
                        "Analytics dashboard",
                        analyticsService.isDashboardCacheLoaded(),
                        Map.of()
                )
        );

        return new AdminCacheStatusDto(
                components.stream().allMatch(AdminCacheComponentDto::loaded),
                components
        );
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh backend caches",
            description = "Refreshes AI allowed values, analytics dashboard cache, skill relation indexes, and job lookup indexes used by filtering and recommendation logic."
    )
    @ApiResponse(responseCode = "200", description = "Caches were refreshed successfully.")
    public Map<String, Object> refreshCaches() {
        aiAllowedValuesService.refresh();
        jobService.refreshSkillRelationIndex();
        jobService.refreshJobLookupIndex();
        analyticsService.refreshDashboardCache();

        return Map.of(
                "status", "OK",
                "refreshedAt", Instant.now().toString()
        );
    }
}
