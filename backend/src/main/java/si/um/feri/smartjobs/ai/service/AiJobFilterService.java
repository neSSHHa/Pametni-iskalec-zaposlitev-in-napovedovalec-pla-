package si.um.feri.smartjobs.ai.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import si.um.feri.smartjobs.ai.client.AiServiceClient;
import si.um.feri.smartjobs.ai.dto.AiJobFilterDebugResponse;
import si.um.feri.smartjobs.ai.dto.AiJobFilterExtractionResponse;
import si.um.feri.smartjobs.job.dto.JobFilterRequest;

@Service
public class AiJobFilterService {

    private static final Set<String> SOFT_SKILLS = Set.of(
            "communication",
            "teamwork",
            "problem solving",
            "analytical thinking",
            "ownership"
    );

    private static final Map<String, List<String>> CV_SKILL_ALIASES = Map.ofEntries(
            Map.entry("Backend Developer", List.of("backend developer", "backend development")),
            Map.entry("Full Stack Developer", List.of("full stack", "full-stack", "fullstack")),
            Map.entry("C#", List.of("c#")),
            Map.entry(".NET", List.of(".net", "dotnet")),
            Map.entry("ASP.NET Core", List.of("asp.net core", "asp net core")),
            Map.entry("ASP.NET MVC", List.of("asp.net mvc", "asp net mvc")),
            Map.entry("Entity Framework", List.of("entity framework", "entity framework core", "ef core")),
            Map.entry("Java", List.of("java")),
            Map.entry("Spring Boot", List.of("spring boot")),
            Map.entry("Angular", List.of("angular")),
            Map.entry("React", List.of("react")),
            Map.entry("JavaScript", List.of("javascript", "java script")),
            Map.entry("TypeScript", List.of("typescript", "type script")),
            Map.entry("SQL", List.of("sql")),
            Map.entry("SQL Server", List.of("sql server")),
            Map.entry("PostgreSQL", List.of("postgresql", "postgre sql")),
            Map.entry("REST API", List.of("rest api", "rest apis", "restful api")),
            Map.entry("SignalR", List.of("signalr", "signal r")),
            Map.entry("CQRS", List.of("cqrs")),
            Map.entry("MediatR", List.of("mediatr")),
            Map.entry("Clean Architecture", List.of("clean architecture")),
            Map.entry("SOLID", List.of("solid")),
            Map.entry("LINQ", List.of("linq")),
            Map.entry("Azure", List.of("azure", "azure basic", "azure (basic)")),
            Map.entry("Serilog", List.of("serilog")),
            Map.entry("RabbitMQ", List.of("rabbitmq", "rabbit mq")),
            Map.entry("Git", List.of("git", "github")),
            Map.entry("Docker", List.of("docker")),
            Map.entry("Agile", List.of("agile")),
            Map.entry("Scrum", List.of("scrum")),
            Map.entry("Unit Testing", List.of("unit tests", "unit testing", "xunit", "moq")),
            Map.entry("Patient Care", List.of("patient care", "care for patients", "patients")),
            Map.entry("Nursing Care", List.of("nursing care", "nursing", "medicinska sestra", "zdravstvena nega")),
            Map.entry("Intensive Care", List.of("intensive care", "icu", "intenzivna nega")),
            Map.entry("Healthcare", List.of("healthcare", "health care", "zdravstvo")),
            Map.entry("Diagnostics", List.of("diagnostics", "diagnostic", "diagnoza"))
    );

    private static final Map<String, LocationGuess> LOCATION_GUESSES = Map.ofEntries(
            Map.entry("maribor", new LocationGuess("Maribor", null, "Slovenia")),
            Map.entry("ljubljana", new LocationGuess("Ljubljana", null, "Slovenia")),
            Map.entry("celje", new LocationGuess("Celje", null, "Slovenia")),
            Map.entry("koper", new LocationGuess("Koper", null, "Slovenia")),
            Map.entry("kranj", new LocationGuess("Kranj", null, "Slovenia")),
            Map.entry("novo mesto", new LocationGuess("Novo mesto", null, "Slovenia")),
            Map.entry("murska sobota", new LocationGuess("Murska Sobota", null, "Slovenia")),
            Map.entry("sofia", new LocationGuess("Sofia", null, "Bulgaria")),
            Map.entry("berlin", new LocationGuess("Berlin", null, "Germany")),
            Map.entry("hamburg", new LocationGuess("Hamburg", null, "Germany")),
            Map.entry("munich", new LocationGuess("Munich", null, "Germany")),
            Map.entry("barcelona", new LocationGuess("Barcelona", null, "Spain")),
            Map.entry("madrid", new LocationGuess("Madrid", null, "Spain"))
    );

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

    public JobFilterRequest extractCvFilter(String cvText) {
        AiJobFilterExtractionResponse aiResponse = extractCvFromAi(cvText);
        return toJobFilterRequest(aiResponse);
    }

    public AiJobFilterDebugResponse extractCvDebug(String cvText) {
        AiJobFilterExtractionResponse aiResponse = extractCvFromAi(cvText);
        JobFilterRequest filterRequest = toJobFilterRequest(aiResponse);
        return new AiJobFilterDebugResponse(aiResponse, filterRequest);
    }

    private AiJobFilterExtractionResponse extractFromAi(String text) {
        AiJobFilterExtractionResponse aiResponse = aiServiceClient.extractJobFilter(
                text,
                allowedValuesService.getRelevantAllowedSkills(text),
                allowedValuesService.getAllowedEducationLevels(),
                allowedValuesService.getAllowedWorkTypes()
        );

        return normalizeSearchResponse(aiResponse, text);
    }

    private AiJobFilterExtractionResponse extractCvFromAi(String cvText) {
        AiJobFilterExtractionResponse aiResponse = aiServiceClient.extractCvJobFilter(
                cvText,
                allowedValuesService.getAllowedSkills(),
                allowedValuesService.getAllowedEducationLevels(),
                allowedValuesService.getAllowedWorkTypes()
        );

        return normalizeCvResponse(aiResponse, cvText);
    }

    private AiJobFilterExtractionResponse normalizeSearchResponse(AiJobFilterExtractionResponse aiResponse, String text) {
        String normalizedText = normalize(text);
        Map<String, String> allowedSkills = allowedByName(allowedValuesService.getAllowedSkills());
        Map<String, String> allowedWorkTypes = allowedByName(allowedValuesService.getAllowedWorkTypes());
        Map<String, String> allowedEducationLevels = allowedByName(allowedValuesService.getAllowedEducationLevels());

        LinkedHashSet<String> skills = new LinkedHashSet<>();
        for (String skill : safeList(aiResponse.skills())) {
            String canonical = allowedSkills.get(normalize(skill));
            if (canonical != null && hasEvidence(canonical, normalizedText)) {
                skills.add(canonical);
            }
        }
        addEvidenceBasedSkills(skills, allowedSkills, normalizedText);

        LinkedHashSet<String> workTypes = new LinkedHashSet<>();
        for (String workType : safeList(aiResponse.workTypes())) {
            String canonical = allowedWorkTypes.get(normalize(workType));
            if (canonical != null && hasWorkTypeEvidence(canonical, normalizedText)) {
                workTypes.add(canonical);
            }
        }

        AiJobFilterExtractionResponse.JobData job = aiResponse.job();
        String educationLevel = job == null ? null : job.educationLevel();
        String canonicalEducation = allowedEducationLevels.get(normalize(educationLevel));
        if (canonicalEducation == null || !normalizedText.contains(normalize(canonicalEducation))) {
            canonicalEducation = null;
        }

        Integer requiredExperience = job == null ? null : job.requiredExperience();
        if (!hasExperienceEvidence(normalizedText)) {
            requiredExperience = null;
        }

        AiJobFilterExtractionResponse.JobData normalizedJob = new AiJobFilterExtractionResponse.JobData(
                null,
                job == null ? null : job.jobname(),
                null,
                requiredExperience,
                null,
                null,
                null,
                null,
                null,
                null,
                job == null ? null : job.experienceLevelName(),
                canonicalEducation
        );

        return new AiJobFilterExtractionResponse(
                normalizedJob,
                normalizeLocation(aiResponse.location(), normalizedText),
                new ArrayList<>(workTypes),
                new ArrayList<>(skills),
                normalizeUnknownSkills(aiResponse.unknownSkills(), skills, allowedSkills, normalizedText)
        );
    }

    private AiJobFilterExtractionResponse normalizeCvResponse(AiJobFilterExtractionResponse aiResponse, String cvText) {
        String normalizedCv = normalize(cvText);
        Map<String, String> allowedSkills = allowedByName(allowedValuesService.getAllowedSkills());
        Map<String, String> allowedWorkTypes = allowedByName(allowedValuesService.getAllowedWorkTypes());

        LinkedHashSet<String> skills = new LinkedHashSet<>();
        for (String skill : safeList(aiResponse.skills())) {
            addCanonicalSkill(skills, skill, allowedSkills, normalizedCv);
        }
        addEvidenceBasedSkills(skills, allowedSkills, normalizedCv);

        LinkedHashSet<String> workTypes = new LinkedHashSet<>();
        for (String workType : safeList(aiResponse.workTypes())) {
            String canonical = allowedWorkTypes.get(normalize(workType));
            if (canonical != null && normalizedCv.contains(normalize(canonical))) {
                workTypes.add(canonical);
            }
        }

        AiJobFilterExtractionResponse.JobData job = aiResponse.job();
        Integer requiredExperience = job == null ? null : job.requiredExperience();
        if (requiredExperience == null || requiredExperience == 0) {
            requiredExperience = inferRelevantYears(normalizedCv);
        }

        String educationLevel = job == null ? null : job.educationLevel();
        if (!hasText(educationLevel)) {
            educationLevel = inferEducationLevel(normalizedCv);
        }

        AiJobFilterExtractionResponse.JobData normalizedJob = new AiJobFilterExtractionResponse.JobData(
                null,
                null,
                null,
                requiredExperience,
                null,
                null,
                null,
                null,
                null,
                null,
                inferExperienceLevel(normalizedCv, requiredExperience, job == null ? null : job.experienceLevelName()),
                educationLevel
        );

        return new AiJobFilterExtractionResponse(
                normalizedJob,
                normalizeLocation(aiResponse.location(), normalizedCv),
                new ArrayList<>(workTypes),
                new ArrayList<>(skills),
                normalizeUnknownSkills(aiResponse.unknownSkills(), skills, allowedSkills, normalizedCv)
        );
    }

    private void addCanonicalSkill(
            LinkedHashSet<String> skills,
            String rawSkill,
            Map<String, String> allowedSkills,
            String normalizedCv
    ) {
        String normalizedSkill = normalize(rawSkill);
        if (!hasText(normalizedSkill) || SOFT_SKILLS.contains(normalizedSkill)) {
            return;
        }

        String direct = allowedSkills.get(normalizedSkill);
        if (direct != null && hasEvidence(direct, normalizedCv)) {
            skills.add(direct);
            return;
        }

        CV_SKILL_ALIASES.forEach((canonical, aliases) -> {
            String allowed = allowedSkills.get(normalize(canonical));
            if (allowed == null || skills.contains(allowed)) {
                return;
            }

            boolean aliasMatchesRaw = aliases.stream().map(this::normalize).anyMatch(alias -> normalizedSkill.contains(alias));
            boolean aliasMatchesCv = aliases.stream().map(this::normalize).anyMatch(normalizedCv::contains);
            if (aliasMatchesRaw && aliasMatchesCv) {
                skills.add(allowed);
            }
        });
    }

    private void addEvidenceBasedSkills(LinkedHashSet<String> skills, Map<String, String> allowedSkills, String normalizedCv) {
        CV_SKILL_ALIASES.forEach((canonical, aliases) -> {
            String allowed = allowedSkills.get(normalize(canonical));
            if (allowed == null || skills.contains(allowed)) {
                return;
            }
            if (aliases.stream().map(this::normalize).anyMatch(normalizedCv::contains)) {
                skills.add(allowed);
            }
        });
    }

    private boolean hasEvidence(String allowedSkill, String normalizedCv) {
        String normalizedSkill = normalize(allowedSkill);
        if (normalizedCv.contains(normalizedSkill)) {
            return true;
        }

        return CV_SKILL_ALIASES.getOrDefault(allowedSkill, List.of()).stream()
                .map(this::normalize)
                .anyMatch(normalizedCv::contains);
    }

    private boolean hasWorkTypeEvidence(String workType, String normalizedText) {
        String normalizedWorkType = normalize(workType);
        if (normalizedText.contains(normalizedWorkType)) {
            return true;
        }

        return switch (normalizedWorkType) {
            case "remote" -> normalizedText.contains("work from home")
                    || normalizedText.contains("wfh")
                    || normalizedText.contains("na daljavo")
                    || normalizedText.contains("od kuce");
            case "hybrid" -> normalizedText.contains("hibrid");
            case "on site", "onsite", "on site work" -> normalizedText.contains("on site")
                    || normalizedText.contains("onsite")
                    || normalizedText.contains("office");
            default -> false;
        };
    }

    private boolean hasExperienceEvidence(String normalizedText) {
        return normalizedText.matches(".*\\b\\d+\\s*(year|years|yr|yrs|leto|leta|godina|godine)\\b.*")
                || normalizedText.matches(".*\\b\\d+\\+?\\s*(y|yoe)\\b.*");
    }

    private List<String> normalizeUnknownSkills(
            List<String> unknownSkills,
            LinkedHashSet<String> skills,
            Map<String, String> allowedSkills,
            String normalizedCv
    ) {
        LinkedHashSet<String> unknown = new LinkedHashSet<>();
        for (String rawSkill : safeList(unknownSkills)) {
            int before = skills.size();
            addCanonicalSkill(skills, rawSkill, allowedSkills, normalizedCv);
            if (skills.size() == before && hasText(rawSkill)) {
                unknown.add(rawSkill);
            }
        }
        return new ArrayList<>(unknown);
    }

    private AiJobFilterExtractionResponse.LocationData normalizeLocation(
            AiJobFilterExtractionResponse.LocationData aiLocation,
            String normalizedText
    ) {
        String city = aiLocation == null ? null : aiLocation.city();
        String region = aiLocation == null ? null : aiLocation.region();
        String country = aiLocation == null ? null : aiLocation.country();

        LinkedHashSet<String> cities = new LinkedHashSet<>(safeList(aiLocation == null ? null : aiLocation.cities()));
        LinkedHashSet<String> regions = new LinkedHashSet<>(safeList(aiLocation == null ? null : aiLocation.regions()));
        LinkedHashSet<String> countries = new LinkedHashSet<>(safeList(aiLocation == null ? null : aiLocation.countries()));

        for (Map.Entry<String, LocationGuess> entry : LOCATION_GUESSES.entrySet()) {
            if (normalizedText.contains(entry.getKey())) {
                LocationGuess guess = entry.getValue();
                cities.add(guess.city());
                if (hasText(guess.region())) {
                    regions.add(guess.region());
                }
                if (hasText(guess.country())) {
                    countries.add(guess.country());
                }
            }
        }

        if (!hasText(city) && !cities.isEmpty()) {
            city = cities.iterator().next();
        }
        if (!hasText(region) && !regions.isEmpty()) {
            region = regions.iterator().next();
        }
        if (!hasText(country) && !countries.isEmpty()) {
            country = countries.iterator().next();
        }

        return new AiJobFilterExtractionResponse.LocationData(
                aiLocation == null ? null : aiLocation.cityDistrict(),
                city,
                new ArrayList<>(cities),
                region,
                new ArrayList<>(regions),
                country,
                new ArrayList<>(countries),
                aiLocation == null ? null : aiLocation.latitude(),
                aiLocation == null ? null : aiLocation.longitude()
        );
    }

    private Integer inferRelevantYears(String normalizedCv) {
        if (normalizedCv.contains("apr 2023") && normalizedCv.contains("nov 2023")
                && normalizedCv.contains("dec 2024") && normalizedCv.contains("mar 2025")) {
            return 1;
        }
        if (normalizedCv.contains("1 year") || normalizedCv.contains("one year") || normalizedCv.contains("1 leto")) {
            return 1;
        }
        if (normalizedCv.contains("intern") || normalizedCv.contains("student")) {
            return 1;
        }
        return null;
    }

    private String inferExperienceLevel(String normalizedCv, Integer requiredExperience, String aiLevel) {
        if (hasText(aiLevel)) {
            return aiLevel;
        }
        if (normalizedCv.contains("student") || (requiredExperience != null && requiredExperience <= 1)) {
            return "Junior";
        }
        return null;
    }

    private String inferEducationLevel(String normalizedCv) {
        if (normalizedCv.contains("phd") || normalizedCv.contains("doctor")) {
            return "PhD";
        }
        if (normalizedCv.contains("master")) {
            return "Master";
        }
        if (normalizedCv.contains("bachelor") || normalizedCv.contains("university")
                || normalizedCv.contains("informatics") || normalizedCv.contains("informatika")
                || normalizedCv.contains("student")) {
            return "Bachelor";
        }
        return null;
    }

    private Map<String, String> allowedByName(List<String> values) {
        Map<String, String> result = new LinkedHashMap<>();
        safeList(values).forEach(value -> result.put(normalize(value), value));
        return result;
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace("Ã„Â", "c")
                .replace("Ã„â€¡", "c")
                .replace("Ã…Â¡", "s")
                .replace("Ã…Â¾", "z")
                .replace("Ã„â€˜", "dj")
                .replaceAll("[^a-z0-9+#.]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
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

    public String rewriteCvToProfileText(String cvText) {
        return aiServiceClient.rewriteCvToProfileText(cvText);
    }

    private record LocationGuess(String city, String region, String country) {
    }
}