package si.um.feri.smartjobs.ai.client;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import si.um.feri.smartjobs.ai.config.AiProperties;
import si.um.feri.smartjobs.ai.dto.AiExtractionRequest;
import si.um.feri.smartjobs.ai.dto.AiJobFilterExtractionResponse;
import si.um.feri.smartjobs.ai.dto.CvRewriteRequest;
import si.um.feri.smartjobs.ai.dto.CvRewriteResponse;

@Component
public class AiServiceClient {

    private final RestTemplate restTemplate;
    private final AiProperties aiProperties;

    public AiServiceClient(RestTemplate restTemplate, AiProperties aiProperties) {
        this.restTemplate = restTemplate;
        this.aiProperties = aiProperties;
    }

    public AiJobFilterExtractionResponse extractJobFilter(
            String text,
            List<String> allowedSkills,
            List<String> allowedEducationLevels,
            List<String> allowedExperienceLevels,
            List<String> allowedWorkTypes,
            List<String> allowedLocations
    ) {
        return postExtraction("/api/ai/jobs/extract", text, allowedSkills, allowedEducationLevels, allowedExperienceLevels, allowedWorkTypes, allowedLocations);
    }

    public AiJobFilterExtractionResponse extractCvJobFilter(
            String cvText,
            List<String> allowedSkills,
            List<String> allowedEducationLevels,
            List<String> allowedExperienceLevels,
            List<String> allowedWorkTypes,
            List<String> allowedLocations
    ) {
        return postExtraction("/api/ai/cv/extract", cvText, allowedSkills, allowedEducationLevels, allowedExperienceLevels, allowedWorkTypes, allowedLocations);
    }

    public String rewriteCvToProfileText(String cvText) {
        CvRewriteResponse response = restTemplate.postForObject(
                aiProperties.serviceUrl() + "/api/ai/cv/rewrite",
                new CvRewriteRequest(cvText),
                CvRewriteResponse.class
        );

        if (response == null || response.text() == null) {
            throw new IllegalStateException("AI service did not return a CV rewrite response.");
        }

        return response.text();
    }

    private AiJobFilterExtractionResponse postExtraction(
            String path,
            String text,
            List<String> allowedSkills,
            List<String> allowedEducationLevels,
            List<String> allowedExperienceLevels,
            List<String> allowedWorkTypes,
            List<String> allowedLocations
    ) {
        AiJobFilterExtractionResponse response = restTemplate.postForObject(
                aiProperties.serviceUrl() + path,
                new AiExtractionRequest(text, allowedSkills, allowedEducationLevels, allowedExperienceLevels, allowedWorkTypes, allowedLocations),
                AiJobFilterExtractionResponse.class
        );

        if (response == null) {
            throw new IllegalStateException("AI service did not return an extraction response.");
        }

        return response;
    }
}
