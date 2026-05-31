package si.um.feri.smartjobs.ai.service;

import org.springframework.stereotype.Service;

import si.um.feri.smartjobs.ai.client.AiServiceClient;
import si.um.feri.smartjobs.ai.dto.AiJobFilterDebugResponse;
import si.um.feri.smartjobs.ai.dto.AiJobFilterExtractionResponse;
import si.um.feri.smartjobs.job.dto.JobFilterRequest;

@Service
public class AiJobFilterService {

    private final AiServiceClient aiServiceClient;
    private final AiAllowedValuesService allowedValuesService;
    private final FastPromptFilterService fastPromptFilterService;

    public AiJobFilterService(
            AiServiceClient aiServiceClient,
            AiAllowedValuesService allowedValuesService,
            FastPromptFilterService fastPromptFilterService
    ) {
        this.aiServiceClient = aiServiceClient;
        this.allowedValuesService = allowedValuesService;
        this.fastPromptFilterService = fastPromptFilterService;
    }

    public JobFilterRequest extractFilter(String text) {
        try {
            AiJobFilterExtractionResponse aiResponse = extractFromAi(text);
            if (isInvalid(aiResponse)) {
                return fastPromptFilterService.buildFilter(text);
            }
            return toJobFilterRequest(aiResponse);
        } catch (RuntimeException e) {
            return fastPromptFilterService.buildFilter(text);
        }
    }

    public AiJobFilterDebugResponse extractDebug(String text) {
        AiJobFilterExtractionResponse aiResponse = extractFromAi(text);
        return new AiJobFilterDebugResponse(aiResponse, toJobFilterRequest(aiResponse));
    }

    public JobFilterRequest extractCvFilter(String cvText) {
        return toJobFilterRequest(extractCvFromAi(cvText));
    }

    public AiJobFilterDebugResponse extractCvDebug(String cvText) {
        AiJobFilterExtractionResponse aiResponse = extractCvFromAi(cvText);
        return new AiJobFilterDebugResponse(aiResponse, toJobFilterRequest(aiResponse));
    }

    private AiJobFilterExtractionResponse extractFromAi(String text) {
        return aiServiceClient.extractJobFilter(
                text,
                allowedValuesService.getAllowedSkills(),
                allowedValuesService.getAllowedEducationLevels(),
                allowedValuesService.getAllowedExperienceLevels(),
                allowedValuesService.getAllowedWorkTypes(),
                allowedValuesService.getAllowedLocations()
        );
    }

    private AiJobFilterExtractionResponse extractCvFromAi(String cvText) {
        return aiServiceClient.extractCvJobFilter(
                cvText,
                allowedValuesService.getAllowedSkills(),
                allowedValuesService.getAllowedEducationLevels(),
                allowedValuesService.getAllowedExperienceLevels(),
                allowedValuesService.getAllowedWorkTypes(),
                allowedValuesService.getAllowedLocations()
        );
    }

    private JobFilterRequest toJobFilterRequest(AiJobFilterExtractionResponse aiResponse) {
        return new JobFilterRequest(
                aiResponse.job() == null ? null : new JobFilterRequest.JobCriteria(
                        aiResponse.job().companyname(),
                        aiResponse.job().jobname(),
                        aiResponse.job().description(),
                        aiResponse.job().requiredExperience(),
                        aiResponse.job().predictedMinSalary(),
                        aiResponse.job().predictedMaxSalary(),
                        aiResponse.job().sourceWebsite(),
                        aiResponse.job().datePosted(),
                        aiResponse.job().minSalary(),
                        aiResponse.job().maxSalary(),
                        aiResponse.job().experienceLevelName(),
                        aiResponse.job().educationLevel()
                ),
                aiResponse.location() == null ? null : new JobFilterRequest.LocationCriteria(
                        aiResponse.location().cityDistrict(),
                        aiResponse.location().city(),
                        aiResponse.location().cities(),
                        aiResponse.location().region(),
                        aiResponse.location().regions(),
                        aiResponse.location().country(),
                        aiResponse.location().countries(),
                        aiResponse.location().latitude(),
                        aiResponse.location().longitude()
                ),
                aiResponse.workTypes(),
                aiResponse.skills()
        );
    }

    private boolean isInvalid(AiJobFilterExtractionResponse aiResponse) {
        if (aiResponse == null) {
            return true;
        }

        return aiResponse.job() == null
                && aiResponse.location() == null
                && isEmpty(aiResponse.workTypes())
                && isEmpty(aiResponse.skills());
    }

    private boolean isEmpty(java.util.List<?> values) {
        return values == null || values.isEmpty();
    }

    public String rewriteCvToProfileText(String cvText) {
        return aiServiceClient.rewriteCvToProfileText(cvText);
    }
}
