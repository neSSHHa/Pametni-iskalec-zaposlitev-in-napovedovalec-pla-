/*
pokliči lokalni AI model
pošlji prompt + dovoljene vrednosti iz baze
vrni AiJobFilterExtractionResponse

*/

package si.um.feri.smartjobs.ai.client;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import si.um.feri.smartjobs.ai.config.AiProperties;
import si.um.feri.smartjobs.ai.dto.AiJobFilterExtractionResponse;

@Component
public class AiServiceClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AiProperties aiProperties;

    public AiServiceClient(RestTemplate restTemplate, ObjectMapper objectMapper, AiProperties aiProperties) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.aiProperties = aiProperties;
    }

    public AiJobFilterExtractionResponse extractJobFilter(
            String text,
            List<String> allowedSkills,
            List<String> allowedEducationLevels,
            List<String> allowedWorkTypes
    ) {
        String prompt = buildPrompt(text, allowedSkills, allowedEducationLevels, allowedWorkTypes);

        OllamaChatRequest request = new OllamaChatRequest(
                aiProperties.model(),
                List.of(new OllamaMessage("user", prompt)),
                false,
                "json",
                Map.of("temperature", 0)
        );

        // dejansko pokliče Ollama
        OllamaChatResponse response = restTemplate.postForObject(
                aiProperties.ollamaUrl() + "/api/chat",
                request,
                OllamaChatResponse.class
        );

        if (response == null || response.message() == null || response.message().content() == null) {
            throw new IllegalStateException("Ollama did not return a valid response.");
        }

        //  vzame AI odgovor in ga spremeni v AiJobFilterExtractionResponse
        return parseAiResponse(response.message().content());
    }

    private String buildPrompt(
            String text,
            List<String> allowedSkills,
            List<String> allowedEducationLevels,
            List<String> allowedWorkTypes
    ) {
        return """
                You convert natural language into a job filter JSON.

                Return ONLY valid JSON. Do not explain. Do not use markdown.

                Allowed skills:
                %s

                Allowed education levels:
                %s

                Allowed work types:
                %s

                Rules:
                - Use only skills from the allowed skills list.
                - Do not add Customer Service, Communication, Teamwork or similar soft skills unless explicitly mentioned.
                - If the user mentions a skill that cannot be mapped to allowed skills, put it in unknownSkills.
                - Map synonyms to the closest allowed value when obvious.
                - Use null for values that are not mentioned.
                - Do not invent salary, location, dates, company names, or experience.
                - requiredExperience is a number. Use years if the user says years.
                - minSalary means the user's minimum acceptable salary.
                - maxSalary means the user's maximum acceptable salary.
                - The output JSON must have exactly this structure:

- requiredExperience MUST be an integer number only.
- Never return text like "3 years" or "2+ years".
- Correct example:
  "requiredExperience": 3
- Incorrect example:
  "requiredExperience": "3 years"
                {
                  "job": {
                    "companyname": null,
                    "jobname": null,
                    "description": null,
                    "requiredExperience": null,
                    "predictedMinSalary": null,
                    "predictedMaxSalary": null,
                    "sourceWebsite": null,
                    "datePosted": null,
                    "minSalary": null,
                    "maxSalary": null,
                    "experienceLevelName": null,
                    "educationLevel": null
                  },
                  "location": {
                    "cityDistrict": null,
                    "city": null,
                    "region": null,
                    "country": null,
                    "latitude": null,
                    "longitude": null
                  },
                  "workTypes": [],
                  "skills": [],
                  "unknownSkills": []
                }

                User text:
                %s
                """.formatted(
                String.join(", ", allowedSkills),
                String.join(", ", allowedEducationLevels),
                String.join(", ", allowedWorkTypes),
                text
        );
    }

    private AiJobFilterExtractionResponse parseAiResponse(String content) {
        String json = extractJson(content);

        try {
            return objectMapper.readValue(json, AiJobFilterExtractionResponse.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not parse AI response: " + content, e);
        }
    }

    private String extractJson(String content) {
        String trimmed = content.trim();

        if (trimmed.startsWith("```")) {
            trimmed = trimmed
                    .replaceFirst("^```json", "")
                    .replaceFirst("^```", "")
                    .replaceFirst("```$", "")
                    .trim();
        }

        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');

        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }

        return trimmed;
    }

    private record OllamaChatRequest(
            String model,
            List<OllamaMessage> messages,
            boolean stream,
            String format,
            Map<String, Object> options
    ) {
    }

    private record OllamaMessage(
            String role,
            String content
    ) {
    }

    private record OllamaChatResponse(
            OllamaMessage message
    ) {
    }
    public String rewriteCvToProfileText(String cvText) {
    String prompt = buildCvRewritePrompt(cvText);

    OllamaChatRequest request = new OllamaChatRequest(
            aiProperties.model(),
            List.of(new OllamaMessage("user", prompt)),
            false,
            null,
            Map.of("temperature", 0)
    );

    OllamaChatResponse response = restTemplate.postForObject(
            aiProperties.ollamaUrl() + "/api/chat",
            request,
            OllamaChatResponse.class
    );

    if (response == null || response.message() == null || response.message().content() == null) {
        throw new IllegalStateException("Ollama did not return a valid CV rewrite response.");
    }

    return response.message().content().trim();
}
private String buildCvRewritePrompt(String cvText) {
    return """
            Convert this CV into one short first-person job search profile.

            The output will be sent to another AI system that extracts job filters.

            Rules:
            - Return only plain text.
            - Write 1 short paragraph only.
            - Maximum 3 sentences.
            - Do not use markdown.
            - Do not use sections.
            - Do not include company names.
            - Do not include school names.
            - Do not include languages unless they are clearly job-relevant technical requirements.
            - Do not include salary unless salary expectation is clearly written.
            - Do not include work type unless remote/hybrid/on-site preference is clearly written.
            - Include candidate location if clearly present.
            - Include total years of relevant experience if present.
            - Include normalized education level if present.
            - Include only important technical skills.
            - Always include candidate location if present in the CV.
            - Normalize obvious terms:
              "Bachelor's degree in Computer Science" -> "Bachelor education level"
              "relational databases" -> "SQL"
              "RESTful APIs" -> "REST APIs"

            Example output:
            I am looking for backend developer jobs in Ljubljana, Slovenia. I have 3 years of experience and a Master education level. My skills are Java, Spring Boot, PostgreSQL, Docker, CI/CD, REST APIs, and Git.

            CV text:
            %s
            """.formatted(cvText);
}
}
