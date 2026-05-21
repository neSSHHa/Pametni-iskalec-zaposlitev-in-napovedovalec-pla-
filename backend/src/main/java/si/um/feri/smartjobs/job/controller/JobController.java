package si.um.feri.smartjobs.job.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import si.um.feri.smartjobs.job.dto.JobDto;
import si.um.feri.smartjobs.job.dto.JobFilterRequest;
import si.um.feri.smartjobs.job.dto.JobSearchResponse;
import si.um.feri.smartjobs.job.dto.TextSearchRequest;
import si.um.feri.smartjobs.job.service.JobService;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobController {
    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    public JobSearchResponse getJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return jobService.findAllPage(page, size);
    }

    @PostMapping("/text-search")
    public List<JobDto> searchJobs(@RequestBody TextSearchRequest request) {
        return jobService.search(request.query());
    }

    @PostMapping("/filter")
    public JobSearchResponse filterJobs(@RequestBody JobFilterRequest request) {
        return jobService.filterResponse(request);
    }
}
