package si.um.feri.smartjobs.ai.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import si.um.feri.smartjobs.ai.dto.AiJobFilterDebugResponse;
import si.um.feri.smartjobs.ai.dto.NaturalLanguageJobFilterRequest;
import si.um.feri.smartjobs.ai.service.AiJobFilterService;
import si.um.feri.smartjobs.ai.service.FastPromptFilterService;
import si.um.feri.smartjobs.job.dto.JobFilterRequest;
import si.um.feri.smartjobs.job.dto.JobSearchResponse;
import si.um.feri.smartjobs.job.service.JobService;


/*
To doda novi endpoint:

POST /api/ai/jobs/filter
Ta endpoint:
sprejme tekst
pokliče AI service
dobi JobFilterRequest
pokliče obstoječi JobService.filter()
vrne List<JobDto>

*/

@RestController
@RequestMapping("/api/ai/jobs")
public class AiJobFilterController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiJobFilterController.class);

    private final AiJobFilterService aiJobFilterService;
    private final FastPromptFilterService fastPromptFilterService;
    private final JobService jobService;

    public AiJobFilterController(
            AiJobFilterService aiJobFilterService,
            FastPromptFilterService fastPromptFilterService,
            JobService jobService
    ) {
        this.aiJobFilterService = aiJobFilterService;
        this.fastPromptFilterService = fastPromptFilterService;
        this.jobService = jobService;
    }

    @PostMapping("/filter")
    public JobSearchResponse filterFromNaturalLanguage(@RequestBody NaturalLanguageJobFilterRequest request) {
        String requestId = MDC.get("requestId");
        String interactionId = MDC.get("interactionId");
        String mode = isThinking(request.mode()) ? "thinking" : "fast";
        LOGGER.info(
                "event=job.prompt.received requestId={} interactionId={} mode={} prompt={}",
                requestId,
                interactionId,
                mode,
                safeForLog(request.text())
        );

        JobFilterRequest filterRequest = "thinking".equals(mode)
                ? aiJobFilterService.extractFilter(request.text())
                : fastPromptFilterService.buildFilter(request.text());
        JobSearchResponse response = jobService.filterResponse(filterRequest);

        LOGGER.info(
                "event=job.prompt.search.completed requestId={} interactionId={} mode={} skills={} workTypes={} location={} returnedJobs={} totalJobs={}",
                requestId,
                interactionId,
                mode,
                filterRequest.skills(),
                filterRequest.workTypes(),
                filterRequest.location(),
                response.jobs().size(),
                response.totalCount()
        );
        return response;
    }

    @PostMapping("/extract")
    public AiJobFilterDebugResponse extractFilterDebug(@RequestBody NaturalLanguageJobFilterRequest request) {
        return aiJobFilterService.extractDebug(request.text());
    }

    private boolean isThinking(String mode) {
        return "thinking".equalsIgnoreCase(mode);
    }

    private String safeForLog(String value) {
        return value == null ? null : value.replaceAll("[\\r\\n\\t]", " ");
    }

}
