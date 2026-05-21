package si.um.feri.smartjobs.ai.controller;

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
        JobFilterRequest filterRequest = isThinking(request.mode())
                ? aiJobFilterService.extractFilter(request.text())
                : fastPromptFilterService.buildFilter(request.text());
        return jobService.filterResponse(filterRequest);
    }

    @PostMapping("/extract")
    public AiJobFilterDebugResponse extractFilterDebug(@RequestBody NaturalLanguageJobFilterRequest request) {
        return aiJobFilterService.extractDebug(request.text());
    }

    private boolean isThinking(String mode) {
        return "thinking".equalsIgnoreCase(mode);
    }

}
