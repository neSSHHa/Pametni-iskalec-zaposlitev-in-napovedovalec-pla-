package si.um.feri.smartjobs.job.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Jobs", description = "Search, browse, and filter job postings stored in the SmartJobs database.")
public class JobController {
    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    @Operation(
            summary = "List job postings",
            description = "Returns a paginated list of job postings with core job details, location data, salary fields, match scores, and related skills."
    )
    @ApiResponse(responseCode = "200", description = "Job postings were returned successfully.")
    public JobSearchResponse getJobs(
            @Parameter(description = "Zero-based page index.", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of jobs to return per page.", example = "50")
            @RequestParam(defaultValue = "50") int size
    ) {
        return jobService.findAllPage(page, size);
    }

    @PostMapping("/text-search")
    @Operation(
            summary = "Search jobs by free text",
            description = "Searches job titles, descriptions, companies, locations, and skill-related text using a simple query string."
    )
    @ApiResponse(responseCode = "200", description = "Matching jobs were returned successfully.")
    public List<JobDto> searchJobs(@RequestBody TextSearchRequest request) {
        return jobService.search(request.query());
    }

    @PostMapping("/filter")
    @Operation(
            summary = "Filter jobs by structured criteria",
            description = "Filters job postings using structured criteria such as company, role, description, salary range, experience level, education level, location, work type, and skills."
    )
    @ApiResponse(responseCode = "200", description = "Filtered job postings were returned successfully.")
    public JobSearchResponse filterJobs(@RequestBody JobFilterRequest request) {
        return jobService.filterResponse(request);
    }
}
