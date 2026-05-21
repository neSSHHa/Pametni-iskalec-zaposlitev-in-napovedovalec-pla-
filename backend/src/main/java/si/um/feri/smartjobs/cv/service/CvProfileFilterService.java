package si.um.feri.smartjobs.cv.service;

import org.springframework.stereotype.Service;

import si.um.feri.smartjobs.ai.service.AiAllowedValuesService;
import si.um.feri.smartjobs.job.dto.JobFilterRequest;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class CvProfileFilterService {

    private static final Set<String> SOFT_SKILLS = Set.of(
            "communication",
            "teamwork",
            "problem solving",
            "analytical thinking",
            "agile",
            "scrum"
    );

    private static final Map<String, List<String>> SKILL_ALIASES = Map.ofEntries(
            Map.entry("C#", List.of("c#")),
            Map.entry(".NET", List.of(".net", "dotnet")),
            Map.entry("ASP.NET Core", List.of("asp.net core", "asp net core", "asp.net")),
            Map.entry("Entity Framework", List.of("entity framework", "ef core", "entity framework core")),
            Map.entry("SQL Server", List.of("sql server")),
            Map.entry("Java", List.of("java")),
            Map.entry("Spring Boot", List.of("spring boot")),
            Map.entry("PostgreSQL", List.of("postgresql", "postgre sql")),
            Map.entry("Angular", List.of("angular")),
            Map.entry("React", List.of("react")),
            Map.entry("JavaScript", List.of("javascript", "java script")),
            Map.entry("TypeScript", List.of("typescript", "type script")),
            Map.entry("SQL", List.of("sql")),
            Map.entry("REST API", List.of("rest api", "rest apis", "restful api")),
            Map.entry("Git", List.of("git", "github")),
            Map.entry("Docker", List.of("docker")),
            Map.entry("Azure", List.of("azure")),
            Map.entry("RabbitMQ", List.of("rabbitmq", "rabbit mq")),
            Map.entry("Unit Testing", List.of("unit tests", "unit testing", "xunit")),
            Map.entry("Integration Testing", List.of("integration tests", "integration testing"))
    );

    private final AiAllowedValuesService allowedValuesService;

    public CvProfileFilterService(AiAllowedValuesService allowedValuesService) {
        this.allowedValuesService = allowedValuesService;
    }

    public JobFilterRequest buildFilter(String cvText) {
        String normalizedCv = normalize(cvText);
        List<String> skills = extractSkills(normalizedCv);

        return new JobFilterRequest(
                new JobFilterRequest.JobCriteria(
                        null,
                        inferRole(normalizedCv, skills),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        inferExperienceLevel(normalizedCv),
                        null
                ),
                null,
                List.of(),
                skills
        );
    }

    private List<String> extractSkills(String normalizedCv) {
        Map<String, String> allowedByName = new LinkedHashMap<>();
        allowedValuesService.getAllowedSkills().forEach(skill -> allowedByName.put(normalize(skill), skill));

        List<String> skills = new ArrayList<>();

        SKILL_ALIASES.forEach((canonicalName, aliases) -> {
            String allowed = allowedByName.get(normalize(canonicalName));
            if (allowed == null || SOFT_SKILLS.contains(normalize(allowed))) {
                return;
            }

            if (aliases.stream().map(this::normalize).anyMatch(normalizedCv::contains)) {
                skills.add(allowed);
            }
        });

        allowedValuesService.getAllowedSkills().stream()
                .filter(skill -> !SOFT_SKILLS.contains(normalize(skill)))
                .filter(skill -> normalizedCv.contains(normalize(skill)))
                .filter(skill -> !skills.contains(skill))
                .forEach(skills::add);

        return skills;
    }

    private String inferRole(String normalizedCv, List<String> skills) {
        int dotnet = score(skills, "C#", ".NET", "ASP.NET Core", "Entity Framework", "SQL Server");
        int java = score(skills, "Java", "Spring Boot", "PostgreSQL");
        int angular = score(skills, "Angular", "TypeScript", "JavaScript");
        int react = score(skills, "React", "JavaScript", "TypeScript");

        boolean backendExperience = normalizedCv.contains("backend developer")
                || normalizedCv.contains("backend system")
                || normalizedCv.contains("backend services")
                || normalizedCv.contains("rest api");
        boolean fullStackExperience = normalizedCv.contains("full stack")
                || normalizedCv.contains("full-stack")
                || normalizedCv.contains("frontend")
                || normalizedCv.contains("angular");

        if (dotnet >= 3 && java >= 2 && fullStackExperience) {
            return "Full Stack .NET Java Developer";
        }
        if (dotnet >= 3 && backendExperience) {
            return ".NET Backend Developer";
        }
        if (java >= 2 && angular >= 2 && fullStackExperience) {
            return "Java Angular Full Stack Developer";
        }
        if (java >= 2 && backendExperience) {
            return "Java Backend Developer";
        }
        if (angular > react) {
            return "Angular Frontend Developer";
        }
        if (react >= 3 && normalizedCv.contains("react")) {
            return "Frontend Developer";
        }

        return "Software Developer";
    }

    private String inferExperienceLevel(String normalizedCv) {
        if (normalizedCv.contains("student")) {
            return "Junior";
        }
        if (normalizedCv.contains("senior") || normalizedCv.contains("lead")) {
            return "Senior";
        }
        return null;
    }

    private int score(List<String> skills, String... wanted) {
        int score = 0;
        for (String value : wanted) {
            if (skills.stream().map(this::normalize).anyMatch(skill -> skill.equals(normalize(value)))) {
                score++;
            }
        }
        return score;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace("č", "c")
                .replace("ć", "c")
                .replace("š", "s")
                .replace("ž", "z")
                .replace("đ", "dj")
                .replaceAll("[^a-z0-9+#.]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
