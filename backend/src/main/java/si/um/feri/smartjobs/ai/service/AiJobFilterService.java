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

    public AiJobFilterService(
            AiServiceClient aiServiceClient,
            AiAllowedValuesService allowedValuesService
    ) {
        this.aiServiceClient = aiServiceClient;
        this.allowedValuesService = allowedValuesService;
    }

    public JobFilterRequest extractFilter(String text) {
        AiJobFilterExtractionResponse aiResponse = extractFromAi(text);
        return toJobFilterRequest(aiResponse);
    }

    public AiJobFilterDebugResponse extractDebug(String text) {
        AiJobFilterExtractionResponse aiResponse = extractFromAi(text);
        JobFilterRequest filterRequest = toJobFilterRequest(aiResponse);

        return new AiJobFilterDebugResponse(aiResponse, filterRequest);
    }

    private AiJobFilterExtractionResponse extractFromAi(String text) {
        return aiServiceClient.extractJobFilter(
                text,
                allowedValuesService.getAllowedSkills(),
                allowedValuesService.getAllowedEducationLevels(),
                allowedValuesService.getAllowedWorkTypes()
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
                        aiResponse.location().region(),
                        aiResponse.location().country(),
                        aiResponse.location().latitude(),
                        aiResponse.location().longitude()
                ),
                aiResponse.workTypes(),
                aiResponse.skills()
        );
    }
    public String rewriteCvToProfileText(String cvText) {
    return aiServiceClient.rewriteCvToProfileText(cvText);
}
}
