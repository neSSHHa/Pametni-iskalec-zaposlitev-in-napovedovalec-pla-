package si.um.feri.smartjobs.admin.controller;

import java.time.Instant;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import si.um.feri.smartjobs.ai.service.AiAllowedValuesService;
import si.um.feri.smartjobs.job.service.JobService;

@RestController
@RequestMapping("/api/admin/cache")
@Tag(name = "Admin", description = "Operational maintenance endpoints for refreshing in-memory caches and search indexes.")
public class AdminCacheController {

    private final AiAllowedValuesService aiAllowedValuesService;
    private final JobService jobService;

    public AdminCacheController(
            AiAllowedValuesService aiAllowedValuesService,
            JobService jobService
    ) {
        this.aiAllowedValuesService = aiAllowedValuesService;
        this.jobService = jobService;
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh backend caches",
            description = "Refreshes AI allowed values, skill relation indexes, and job lookup indexes used by filtering and recommendation logic."
    )
    @ApiResponse(responseCode = "200", description = "Caches were refreshed successfully.")
    public Map<String, Object> refreshCaches() {
        aiAllowedValuesService.refresh();
        jobService.refreshSkillRelationIndex();
        jobService.refreshJobLookupIndex();

        return Map.of(
                "status", "OK",
                "refreshedAt", Instant.now().toString()
        );
    }
}
