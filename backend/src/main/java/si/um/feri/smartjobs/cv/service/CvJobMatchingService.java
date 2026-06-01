package si.um.feri.smartjobs.cv.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(CvJobMatchingService.class);

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
        String requestId = MDC.get("requestId");
        String interactionId = MDC.get("interactionId");
        String normalizedMode = isThinking(mode) ? "thinking" : "fast";
        LOGGER.info(
                "event=cv.upload.received requestId={} interactionId={} mode={}",
                requestId,
                interactionId,
                normalizedMode
        );

        String extractedText = cvTextExtractionService.extractText(file);

        JobFilterRequest filterRequest = "thinking".equals(normalizedMode)
                ? aiJobFilterService.extractCvFilter(extractedText)
                : cvProfileFilterService.buildFilter(extractedText);
        LOGGER.info(
                "event=cv.filter.extracted requestId={} interactionId={} mode={} skills={} workTypes={} location={}",
                requestId,
                interactionId,
                normalizedMode,
                filterRequest.skills(),
                filterRequest.workTypes(),
                filterRequest.location()
        );

        JobSearchResponse rankedJobs = jobService.filterResponse(filterRequest);
        LOGGER.info(
                "event=cv.search.completed requestId={} interactionId={} mode={} returnedJobs={} totalJobs={}",
                requestId,
                interactionId,
                normalizedMode,
                rankedJobs.jobs().size(),
                rankedJobs.totalCount()
        );

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
