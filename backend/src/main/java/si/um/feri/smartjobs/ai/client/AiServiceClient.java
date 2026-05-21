/*
pokliči lokalni AI model
pošlji prompt + dovoljene vrednosti iz baze
vrni AiJobFilterExtractionResponse

*/

package si.um.feri.smartjobs.ai.client;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import si.um.feri.smartjobs.ai.config.AiProperties;
import si.um.feri.smartjobs.ai.dto.AiJobFilterExtractionResponse;

@Component
public class AiServiceClient {
    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+(?:[.,]\\d+)?");

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

    public AiJobFilterExtractionResponse extractCvJobFilter(
            String cvText,
            List<String> allowedSkills,
            List<String> allowedEducationLevels,
            List<String> allowedWorkTypes
    ) {
        String prompt = buildCvFilterPrompt(cvText, allowedSkills, allowedEducationLevels, allowedWorkTypes);

        OllamaChatRequest request = new OllamaChatRequest(
                aiProperties.model(),
                List.of(new OllamaMessage("user", prompt)),
                false,
                "json",
                Map.of("temperature", 0)
        );

        OllamaChatResponse response = restTemplate.postForObject(
                aiProperties.ollamaUrl() + "/api/chat",
                request,
                OllamaChatResponse.class
        );

        if (response == null || response.message() == null || response.message().content() == null) {
            throw new IllegalStateException("Ollama did not return a valid CV filter response.");
        }

        return parseAiResponse(response.message().content());
    }

    private String buildPrompt(
            String text,
            List<String> allowedSkills,
            List<String> allowedEducationLevels,
            List<String> allowedWorkTypes
    ) {
        return """
                You convert a user job-search request into a precise job filter JSON.

                Return ONLY valid JSON. Do not explain. Do not use markdown.
                The request can be written in Serbian, Croatian, Bosnian, Slovenian, English, German, or mixed language.
                Translate the meaning internally to English before choosing values.

                Allowed skills:
                %s

                Allowed education levels:
                %s

                Allowed work types:
                %s

                Rules:
                - Use only skills from the allowed skills list.
                - Put the desired profession/title/domain in job.jobname, for example "React Frontend Developer", "Java Backend Developer", "Data Analyst", "UX Designer".
                - Keep job.description null unless the user explicitly asks for a keyword that is not a title, skill, company, salary, location, work type, education or experience.
                - Do not add Customer Service, Communication, Teamwork or similar soft skills unless explicitly mentioned.
                - If the user mentions a skill that cannot be mapped to allowed skills, put it in unknownSkills.
                - Map synonyms to the closest allowed value when obvious.
                - Map "frontend", "front-end", "UI", "React UI" to frontend-style titles and allowed frontend skills.
                - Map "backend", "back-end", "server-side", "API" to backend-style titles and allowed backend skills.
                - Map local words such as "programer", "razvijalec", "inzenjer", "hibrid", "od kuce", "na daljavo" to their English meaning.
                - Use null for values that are not mentioned.
                - Do not invent salary, location, dates, company names, or experience.
                - requiredExperience is a number. Use years if the user says years.
                - minSalary means the user's minimum acceptable salary.
                - maxSalary means the user's maximum acceptable salary.
                - Do not put coordinates unless the user explicitly writes exact numeric coordinates.
                - If a country/city is mentioned, write its normal English name.
                - If the user asks for remote jobs, put "Remote" in workTypes and do not force a city.
                - If the user asks for hybrid jobs in a city/country, include both workTypes and location.
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

    private String buildCvFilterPrompt(
            String cvText,
            List<String> allowedSkills,
            List<String> allowedEducationLevels,
            List<String> allowedWorkTypes
    ) {
        return """
                You extract a job-matching filter from a candidate CV.

                Return ONLY valid JSON. Do not explain. Do not use markdown.
                The CV can be written in Serbian, Croatian, Bosnian, Slovenian, English, German, or mixed language.
                Translate the meaning internally to English before choosing values.

                Allowed skills:
                %s

                Allowed education levels:
                %s

                Allowed work types:
                %s

                Goal:
                - Build a filter that finds jobs suitable for this candidate.
                - Preserve the real profession/domain from the CV.
                - Do not assume the candidate is a software engineer unless the CV says so.

                Rules:
                - Use only exact values from the allowed skills list in skills.
                - For CV extraction keep job.jobname null unless the CV explicitly contains a desired target job title.
                - If the CV contains roles/professions such as Backend Developer or Full Stack Developer, put them in skills only if they exist in the allowed skills list.
                - Map aliases to allowed values, for example REST APIs -> REST API, Entity Framework Core or EF Core -> Entity Framework, Azure basic -> Azure, Agile/Scrum -> Agile and Scrum.
                - Do not return duplicate skills.
                - Put total relevant years into job.requiredExperience as an integer number of years.
                - Use experienceLevelName only when the level is clear: Intern, Entry, Junior, Mid, Senior, Lead, Manager.
                - Use educationLevel only if it maps clearly to one allowed education level.
                - Put workTypes only if the CV explicitly states remote, hybrid, on-site, field, student, full-time or part-time preference.
                - Put current city/country in location when clearly present in the CV, for example Maribor -> city Maribor and country Slovenia.
                - Never put previous employers into companyname.
                - Never invent salary, dates, sourceWebsite, latitude or longitude.
                - Keep job.description null unless the CV contains an important role keyword that cannot fit into job.jobname or skills.
                - If a skill is important but cannot be mapped to allowed skills, put it in unknownSkills.
                - The output JSON must have exactly this structure:

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

                CV text:
                %s
                """.formatted(
                String.join(", ", allowedSkills),
                String.join(", ", allowedEducationLevels),
                String.join(", ", allowedWorkTypes),
                cvText
        );
    }

    private AiJobFilterExtractionResponse parseAiResponse(String content) {
        String json = extractJson(content);

        try {
            JsonNode normalized = normalizeAiJson(json);
            return objectMapper.treeToValue(normalized, AiJobFilterExtractionResponse.class);
        } catch (JsonProcessingException | NumberFormatException e) {
            throw new IllegalStateException("Could not parse AI response: " + content, e);
        }
    }

    private JsonNode normalizeAiJson(String json) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(json);
        if (!(root instanceof ObjectNode rootObject)) {
            return root;
        }

        ObjectNode job = objectNode(rootObject, "job");
        if (job != null) {
            nullBlankText(job, "companyname");
            nullBlankText(job, "jobname");
            nullBlankText(job, "description");
            nullBlankText(job, "sourceWebsite");
            nullBlankText(job, "experienceLevelName");
            nullBlankText(job, "educationLevel");
            coerceInteger(job, "requiredExperience");
            coerceDecimal(job, "predictedMinSalary");
            coerceDecimal(job, "predictedMaxSalary");
            coerceDecimal(job, "minSalary");
            coerceDecimal(job, "maxSalary");
            coerceIsoDate(job, "datePosted");
        }

        ObjectNode location = objectNode(rootObject, "location");
        if (location != null) {
            nullBlankText(location, "cityDistrict");
            nullBlankText(location, "city");
            nullBlankText(location, "region");
            nullBlankText(location, "country");
            coerceDecimal(location, "latitude");
            coerceDecimal(location, "longitude");
        }

        ensureArray(rootObject, "workTypes");
        ensureArray(rootObject, "skills");
        ensureArray(rootObject, "unknownSkills");

        return rootObject;
    }

    private ObjectNode objectNode(ObjectNode parent, String field) {
        JsonNode node = parent.get(field);
        return node instanceof ObjectNode objectNode ? objectNode : null;
    }

    private void ensureArray(ObjectNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            node.putArray(field);
            return;
        }
        if (!value.isArray()) {
            node.putArray(field);
        }
    }

    private void nullBlankText(ObjectNode node, String field) {
        JsonNode value = node.get(field);
        if (value != null && value.isTextual() && value.asText().isBlank()) {
            node.putNull(field);
        }
    }

    private void coerceInteger(ObjectNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.isInt()) {
            return;
        }

        if (value.isNumber()) {
            node.put(field, value.intValue());
            return;
        }

        String number = firstNumber(value.asText(""));
        if (number == null) {
            node.putNull(field);
            return;
        }

        node.put(field, (int) Math.round(Double.parseDouble(normalizeNumber(number))));
    }

    private void coerceDecimal(ObjectNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.isNumber()) {
            return;
        }

        String number = firstNumber(value.asText(""));
        if (number == null) {
            node.putNull(field);
            return;
        }

        node.put(field, new BigDecimal(normalizeNumber(number)));
    }

    private void coerceIsoDate(ObjectNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return;
        }

        String text = value.asText("");
        if (!text.matches("\\d{4}-\\d{2}-\\d{2}")) {
            node.putNull(field);
        }
    }

    private String firstNumber(String value) {
        Matcher matcher = NUMBER_PATTERN.matcher(value == null ? "" : value);
        return matcher.find() ? matcher.group() : null;
    }

    private String normalizeNumber(String value) {
        if (value.matches("-?\\d{1,3},\\d{3}")) {
            return value.replace(",", "");
        }

        return value.replace(",", ".");
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
You MUST base the response ONLY on the provided CV text.

Do not reuse information from previous requests.
Do not assume the profession is software engineering.
The CV may belong to any profession.

Examples:
- healthcare
- law
- accounting
- education
- logistics
- engineering
- administration

Never change the profession/domain from the CV.


If the CV is medical, legal, finance, teaching or another field, preserve that field exactly.

Never invent technologies, programming languages or software skills unless explicitly written in the CV.

Convert this CV into one short first-person job search profile.

The output will be sent to another AI system that extracts job filters.

- The profile should sound like a concise job-search summary, not a biography.
- Prefer concise factual statements over descriptive storytelling.

Rules:
- Always write in first person using "I".
- Never refer to the candidate in third person.
- Return only plain text.
- Write 1 short paragraph only.
- Maximum 3 sentences.
- Do not use markdown.
- Do not use sections.
- Do not include company names.
- Do not include school names.
- Do not include languages unless they are clearly job-relevant.
- Do not include salary unless salary expectation is clearly written.
- Do not include work type unless remote/hybrid/on-site preference is clearly written.
- Include candidate location if clearly present.
- Include total years of relevant experience if present.
- Include normalized education level if present.
- Include only important professional skills explicitly mentioned in the CV.
- Always include candidate location if present in the CV.



CV text:
%s
""".formatted(cvText);
}
}
