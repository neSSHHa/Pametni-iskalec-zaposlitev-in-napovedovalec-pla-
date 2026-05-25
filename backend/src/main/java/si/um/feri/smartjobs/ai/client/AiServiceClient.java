/*
poklici AI model prek OpenRouter
poslji prompt + dovoljene vrednosti iz baze
vrni AiJobFilterExtractionResponse
*/

package si.um.feri.smartjobs.ai.client;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
            List<String> allowedExperienceLevels,
            List<String> allowedWorkTypes
    ) {
        String prompt = buildPrompt(text, "prompt", allowedSkills, allowedEducationLevels, allowedExperienceLevels, allowedWorkTypes);

        OpenRouterChatRequest request = new OpenRouterChatRequest(
                aiProperties.model(),
                List.of(new OpenRouterMessage("user", prompt)),
                0,
                Map.of("type", "json_object")
        );

        return parseAiResponse(callOpenRouter(request, "job filter"));
    }

    public AiJobFilterExtractionResponse extractCvJobFilter(
            String cvText,
            List<String> allowedSkills,
            List<String> allowedEducationLevels,
            List<String> allowedExperienceLevels,
            List<String> allowedWorkTypes
    ) {
        String prompt = buildPrompt(cvText, "cv", allowedSkills, allowedEducationLevels, allowedExperienceLevels, allowedWorkTypes);

        OpenRouterChatRequest request = new OpenRouterChatRequest(
                aiProperties.model(),
                List.of(new OpenRouterMessage("user", prompt)),
                0,
                Map.of("type", "json_object")
        );

        return parseAiResponse(callOpenRouter(request, "CV filter"));
    }

    public String rewriteCvToProfileText(String cvText) {
        String prompt = buildCvRewritePrompt(cvText);

        OpenRouterChatRequest request = new OpenRouterChatRequest(
                aiProperties.model(),
                List.of(new OpenRouterMessage("user", prompt)),
                0,
                null
        );

        return callOpenRouter(request, "CV rewrite").trim();
    }

    private String buildPrompt(
            String input,
            String type,
            List<String> allowedSkills,
            List<String> allowedEducationLevels,
            List<String> allowedExperienceLevels,
            List<String> allowedWorkTypes
    ) {
        String typeRules = "cv".equals(type) ? cvRules() : promptRules();

        return """
                You extract a job-search filter from user input.

                Input type: %s
                Return ONLY strict JSON. No markdown. No explanation.
                The response must be parseable by Jackson ObjectMapper.
                Do not stop before closing all braces and brackets.
                Do not add trailing commas.
                Do not output partial JSON.

                Critical rules:
                - The app domain is unknown. It can be IT, healthcare, finance, logistics, education, construction, USA jobs, Europe jobs, anything.
                - The controlled database values below are the source of truth for skills, workTypes, educationLevel and experienceLevelName.
                - Use ONLY values from the allowed lists for skills, workTypes, educationLevel and experienceLevelName.
                - job.jobname and location are free-text extraction fields: infer them from the input and normalize to English when clear.
                - Translate city/country names to normal English names when clear, for example Wien -> Vienna, Beograd -> Belgrade.
                - If a controlled value is not in the allowed list, do not put it in skills/workTypes/educationLevel/experienceLevelName.
                - If the user clearly mentions an important skill/title that is not allowed, put it into unknownSkills.
                - Extract skills exhaustively, not selectively.
                - skills must contain every allowed skill that is directly mentioned or clearly implied by the text.
                - Adding a soft skill such as Communication must never remove professional skills such as Nurse, Registered Nurse, Dental Nurse, Patient Care, etc.
                - Do not return only one skill if multiple allowed skills are supported by the text.
                - If a soft/general skill is mentioned and allowed, include it in addition to all specific professional skills supported by the text.
                - Do not invent salary, company, dates, coordinates, location or experience.
                - requiredExperience must be an integer number of years or null.
                - If the text contains salary/plata/placa/zarada/pay such as "salary 5000 eur", "plata od 3500", "$70000", put that numeric amount into job.minSalary.
                - If the text contains a salary range such as "5000-7000", put the lower number into minSalary and higher number into maxSalary.
                - Never leave minSalary null when an explicit salary number is written.
                - job.jobname should be concise, for example "Nurse", "Java Developer", "Accountant", "Warehouse Worker".
                - If exactly one city/region/country is mentioned, fill both the single field and the matching list field.
                - If multiple cities are mentioned, put all normalized English names into location.cities and put the first one into location.city.
                - If multiple regions are mentioned, put all normalized English names into location.regions and put the first one into location.region.
                - If multiple countries are mentioned, put all normalized English names into location.countries and put the first one into location.country.
                - Never drop additional cities/regions/countries just because one single location field is already filled.

                Type-specific rules:
                %s

                Controlled database values:
                skills: %s
                workTypes: %s
                educationLevels: %s
                experienceLevels: %s

                Exact output shape:
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
                    "cities": [],
                    "region": null,
                    "regions": [],
                    "country": null,
                    "countries": [],
                    "latitude": null,
                    "longitude": null
                  },
                  "workTypes": [],
                  "skills": [],
                  "unknownSkills": []
                }

                Text:
                %s
                """.formatted(
                type,
                typeRules,
                jsonArray(allowedSkills),
                jsonArray(allowedWorkTypes),
                jsonArray(allowedEducationLevels),
                jsonArray(allowedExperienceLevels),
                input
        );
    }

    private String promptRules() {
        return """
                - Interpret the text as a user's desired job search.
                - Extract requested profession/role into job.jobname when clear.
                - Extract requested location into location when clear.
                - Extract requested work type, education, experience level, experience years and salary when clear.
                - Do not treat the user's current/past employer as companyname unless they explicitly want jobs at that company.""";
    }

    private String cvRules() {
        return """
                - Interpret the text as a candidate CV/resume/profile, not as a direct job ad.
                - Build a filter for jobs suitable for the candidate.
                - Never put previous employers, schools or project names into companyname.
                - job.jobname should be the candidate's target role if explicitly stated; otherwise infer the broad suitable profession from the CV.
                - skills must include all allowed skills supported by the candidate's experience, education, certifications, tools, responsibilities or stated strengths.
                - requiredExperience should be total relevant professional experience in years when the CV supports it.
                - If total years are not written directly, estimate requiredExperience from dated work/project/internship periods in the CV.
                - Count relevant periods from start date to end date/present and merge overlapping periods.
                - Convert total relevant duration to integer years using these strict thresholds: less than 6 months = 0; 6 to 17 months = 1; 18 to 29 months = 2; continue the same pattern.
                - Understand month names and short forms across common CV languages, for example Jan, Feb, Apr, Dec, Januar, April, Dezember, mar, nov, avg, sep.
                - If date ranges are present but ambiguous, prefer a conservative lower estimate. If experience cannot be inferred at all, use null.
                - experienceLevelName should reflect the candidate level only if clear from the CV or years of experience.
                - educationLevel should use an allowed value only when the CV clearly supports it.
                - location should be the candidate's current/residence/preferred location when clear; do not use old employer locations unless they are clearly current.
                - workTypes should be filled only if the CV states a preference or availability for remote/hybrid/on-site/full-time/part-time/etc.
                - Salary fields should be filled only if the CV states salary expectation or desired pay.""";
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

                CV text:
                %s
                """.formatted(cvText);
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
            ensureArray(location, "cities");
            ensureArray(location, "regions");
            ensureArray(location, "countries");
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
        if (start < 0) {
            return trimmed;
        }

        boolean inString = false;
        boolean escaped = false;
        int depth = 0;

        for (int i = start; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (ch == '\\') {
                escaped = true;
                continue;
            }

            if (ch == '"') {
                inString = !inString;
                continue;
            }

            if (inString) {
                continue;
            }

            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;

                if (depth == 0) {
                    return trimmed.substring(start, i + 1);
                }
            }
        }

        return trimmed.substring(start);
    }

    private String jsonArray(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize allowed AI values.", e);
        }
    }

    private String callOpenRouter(OpenRouterChatRequest request, String purpose) {
        if (aiProperties.openrouterApiKey() == null || aiProperties.openrouterApiKey().isBlank()) {
            throw new IllegalStateException("OPENROUTER_API_KEY environment variable is required for AI " + purpose + ".");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiProperties.openrouterApiKey());

        if (aiProperties.openrouterReferer() != null && !aiProperties.openrouterReferer().isBlank()) {
            headers.set("HTTP-Referer", aiProperties.openrouterReferer());
        }

        if (aiProperties.openrouterTitle() != null && !aiProperties.openrouterTitle().isBlank()) {
            headers.set("X-Title", aiProperties.openrouterTitle());
        }

        OpenRouterChatResponse response = restTemplate.postForObject(
                aiProperties.openrouterUrl() + "/chat/completions",
                new HttpEntity<>(request, headers),
                OpenRouterChatResponse.class
        );

        if (response != null && response.error() != null) {
            throw new IllegalStateException(
                    "OpenRouter error for " + purpose + ": "
                            + response.error().message()
                            + " (code: " + response.error().code() + ")"
            );
        }

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("OpenRouter did not return a valid " + purpose + " response.");
        }

        OpenRouterMessage message = response.choices().get(0).message();
        if (message == null || message.content() == null) {
            throw new IllegalStateException("OpenRouter response did not contain content for " + purpose + ".");
        }

        return message.content();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record OpenRouterChatRequest(
            String model,
            List<OpenRouterMessage> messages,
            double temperature,
            @JsonProperty("response_format") Map<String, String> responseFormat
    ) {
    }

    private record OpenRouterMessage(
            String role,
            String content
    ) {
    }

    private record OpenRouterChatResponse(
            List<OpenRouterChoice> choices,
            OpenRouterError error
    ) {
    }

    private record OpenRouterChoice(
            OpenRouterMessage message
    ) {
    }

    private record OpenRouterError(
            String message,
            Object code
    ) {
    }
}