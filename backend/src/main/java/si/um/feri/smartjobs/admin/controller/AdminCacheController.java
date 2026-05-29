package si.um.feri.smartjobs.admin.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import si.um.feri.smartjobs.ai.service.AiAllowedValuesService;
import si.um.feri.smartjobs.job.service.JobService;

@RestController
@RequestMapping("/api/admin/cache")
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
