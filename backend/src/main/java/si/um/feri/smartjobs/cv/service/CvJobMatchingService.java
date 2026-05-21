package si.um.feri.smartjobs.cv.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import si.um.feri.smartjobs.cv.dto.CvJobMatchResponse;
import si.um.feri.smartjobs.ai.service.AiJobFilterService;
import si.um.feri.smartjobs.job.dto.JobFilterRequest;
import si.um.feri.smartjobs.job.dto.JobSearchResponse;
import si.um.feri.smartjobs.job.service.JobService;
import si.um.feri.smartjobs.ai.dto.AiJobFilterDebugResponse;

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

    public CvJobMatchResponse matchJobs(MultipartFile file, String mode) {
        String extractedText = cvTextExtractionService.extractText(file);

        JobFilterRequest filterRequest = isThinking(mode)
                ? aiJobFilterService.extractCvFilter(extractedText)
                : cvProfileFilterService.buildFilter(extractedText);
        JobSearchResponse rankedJobs = jobService.filterResponse(filterRequest);

        return new CvJobMatchResponse(
                file.getOriginalFilename(),
                file.getContentType(),
                extractedText,
                filterRequest,
                rankedJobs.jobs(),
                rankedJobs.totalCount(),
                rankedJobs.page(),
                rankedJobs.size(),
                rankedJobs.hasMore(),
                rankedJobs.averageMatch()
        );
    }

    private boolean isThinking(String mode) {
        return "thinking".equalsIgnoreCase(mode);
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
