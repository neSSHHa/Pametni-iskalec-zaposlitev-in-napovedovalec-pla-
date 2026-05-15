package si.um.feri.smartjobs.job.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import si.um.feri.smartjobs.job.dto.JobDto;
import si.um.feri.smartjobs.job.dto.JobFilterRequest;
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
    public List<JobDto> getJobs() {
        return jobService.findAll();
    }

    @PostMapping("/text-search")
    public List<JobDto> searchJobs(@RequestBody TextSearchRequest request) {
        return jobService.search(request.query());
    }

    @PostMapping("/filter")
    public List<JobDto> filterJobs(@RequestBody JobFilterRequest request) {
        return jobService.filter(request);
    }
}
