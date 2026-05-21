package si.um.feri.smartjobs.cv.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import si.um.feri.smartjobs.cv.dto.CvJobMatchResponse;
import si.um.feri.smartjobs.ai.service.AiJobFilterService;
import si.um.feri.smartjobs.job.dto.JobDto;
import si.um.feri.smartjobs.job.dto.JobFilterRequest;
import si.um.feri.smartjobs.job.service.JobService;
import si.um.feri.smartjobs.ai.dto.AiJobFilterDebugResponse;

import java.util.List;

@Service
public class CvJobMatchingService {

    private final CvTextExtractionService cvTextExtractionService;
    private final CvProfileFilterService cvProfileFilterService;
    private final AiJobFilterService aiJobFilterService;
    private final JobService jobService;

    public CvJobMatchingService(
            CvTextExtractionService cvTextExtractionService,
            CvProfileFilterService cvProfileFilterService,
            AiJobFilterService aiJobFilterService,
            JobService jobService
    ) {
        this.cvTextExtractionService = cvTextExtractionService;
        this.cvProfileFilterService = cvProfileFilterService;
        this.aiJobFilterService = aiJobFilterService;
        this.jobService = jobService;
    }

    public CvJobMatchResponse matchJobs(MultipartFile file) {
        String extractedText = cvTextExtractionService.extractText(file);

        JobFilterRequest filterRequest = cvProfileFilterService.buildFilter(extractedText);
        List<JobDto> rankedJobs = jobService.filter(filterRequest);
        List<JobDto> jobs = rankedJobs.stream()
                .filter(job -> job.matchScore() >= 50)
                .toList();

        if (jobs.isEmpty()) {
            jobs = rankedJobs.stream().limit(100).toList();
        }

        return new CvJobMatchResponse(
                file.getOriginalFilename(),
                file.getContentType(),
                extractedText,
                filterRequest,
                jobs
        );
    }

    public AiJobFilterDebugResponse extractFilterDebug(MultipartFile file) {
        String extractedText = cvTextExtractionService.extractText(file);
        return aiJobFilterService.extractCvDebug(extractedText);
    }

    // Testing helper for checking how the CV is rewritten into a search profile.
    public String rewriteCvToProfileText(String extractedText) {
        return aiJobFilterService.rewriteCvToProfileText(extractedText);
    }
}
