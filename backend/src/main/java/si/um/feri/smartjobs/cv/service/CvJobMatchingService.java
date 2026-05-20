package si.um.feri.smartjobs.cv.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import si.um.feri.smartjobs.ai.service.AiJobFilterService;
import si.um.feri.smartjobs.cv.dto.CvJobMatchResponse;
import si.um.feri.smartjobs.job.dto.JobDto;
import si.um.feri.smartjobs.job.dto.JobFilterRequest;
import si.um.feri.smartjobs.job.service.JobService;
import si.um.feri.smartjobs.ai.dto.AiJobFilterDebugResponse;

import java.util.List;

@Service
public class CvJobMatchingService {

    private final CvTextExtractionService cvTextExtractionService;
    private final AiJobFilterService aiJobFilterService;
    private final JobService jobService;

    public CvJobMatchingService(
            CvTextExtractionService cvTextExtractionService,
            AiJobFilterService aiJobFilterService,
            JobService jobService
    ) {
        this.cvTextExtractionService = cvTextExtractionService;
        this.aiJobFilterService = aiJobFilterService;
        this.jobService = jobService;
    }

    public CvJobMatchResponse matchJobs(MultipartFile file) {
String extractedText = cvTextExtractionService.extractText(file);
String profileText = aiJobFilterService.rewriteCvToProfileText(extractedText);

JobFilterRequest filterRequest = aiJobFilterService.extractFilter(profileText);
   List<JobDto> jobs = jobService.filter(filterRequest);

        return new CvJobMatchResponse(
                file.getOriginalFilename(),
                file.getContentType(),
                extractedText,
                filterRequest,
                jobs
        );
    }

    private String buildCvPrompt(String extractedText) {
        return  """
            This text is a candidate CV, not a job search request.

            Extract a job filter based on the candidate profile.

            IMPORTANT:
            - If the CV says the candidate has X years of experience, put X into job.requiredExperience.
            - job.requiredExperience means candidate years of experience for this CV matching flow.
            - Example: "2 years of experience" -> "requiredExperience": 2

            Use:
            - skills explicitly present in the CV
            - education level if present
            - years of experience if present
            - work type only if clearly mentioned

            Do not:
            - treat previous employers as requested companies
            - invent salary
            - invent preferred location
            - add soft skills unless explicitly written

            CV text:
            %s
            """.formatted(extractedText);
    }
   public AiJobFilterDebugResponse extractFilterDebug(MultipartFile file) {
    String extractedText = cvTextExtractionService.extractText(file);
    String profileText = aiJobFilterService.rewriteCvToProfileText(extractedText);

    return aiJobFilterService.extractDebug(profileText);
}
//ova e za tetsing za cv to query like 
public String rewriteCvToProfileText(String extractedText) {
    return aiJobFilterService.rewriteCvToProfileText(extractedText);
}
}
