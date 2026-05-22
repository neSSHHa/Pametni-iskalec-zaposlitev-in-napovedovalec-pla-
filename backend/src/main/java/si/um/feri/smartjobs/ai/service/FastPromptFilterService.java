package si.um.feri.smartjobs.ai.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import si.um.feri.smartjobs.job.dto.JobFilterRequest;

@Service
public class FastPromptFilterService {

    private static final Map<String, List<String>> SKILL_ALIASES = Map.ofEntries(
            Map.entry("C#", List.of("c#")),
            Map.entry(".NET", List.of(".net", "dotnet")),
            Map.entry("ASP.NET Core", List.of("asp.net core", "asp net core", "asp.net")),
            Map.entry("Entity Framework", List.of("entity framework", "ef core", "entity framework core")),
            Map.entry("SQL Server", List.of("sql server")),
            Map.entry("REST API", List.of("rest api", "rest apis", "restful api")),
            Map.entry("Docker", List.of("docker")),
            Map.entry("Git", List.of("git", "github")),
            Map.entry("Java", List.of("java")),
            Map.entry("Spring Boot", List.of("spring boot")),
            Map.entry("PostgreSQL", List.of("postgresql", "postgre sql")),
            Map.entry("React", List.of("react")),
            Map.entry("Angular", List.of("angular")),
            Map.entry("JavaScript", List.of("javascript", "java script")),
            Map.entry("TypeScript", List.of("typescript", "type script")),
            Map.entry("Python", List.of("python")),
            Map.entry("SQL", List.of("sql"))
    );

    private static final Map<String, String> COUNTRY_ALIASES = Map.ofEntries(
            Map.entry("slovenia", "Slovenia"),
            Map.entry("slovenija", "Slovenia"),
            Map.entry("germany", "Germany"),
            Map.entry("nemcija", "Germany"),
            Map.entry("austria", "Austria"),
            Map.entry("avstrija", "Austria"),
            Map.entry("croatia", "Croatia"),
            Map.entry("hrvaska", "Croatia"),
            Map.entry("serbia", "Serbia"),
            Map.entry("srbija", "Serbia")
    );

    private static final Map<String, String> CITY_ALIASES = Map.ofEntries(
            Map.entry("ljubljana", "Ljubljana"),
            Map.entry("maribor", "Maribor"),
            Map.entry("celje", "Celje"),
            Map.entry("koper", "Koper"),
            Map.entry("kranj", "Kranj"),
            Map.entry("novo mesto", "Novo mesto"),
            Map.entry("murska sobota", "Murska Sobota"),
            Map.entry("berlin", "Berlin"),
            Map.entry("munich", "Munich"),
            Map.entry("hamburg", "Hamburg")
    );

    private final AiAllowedValuesService allowedValuesService;

    public FastPromptFilterService(AiAllowedValuesService allowedValuesService) {
        this.allowedValuesService = allowedValuesService;
    }

    public JobFilterRequest buildFilter(String text) {
        String normalized = normalize(text);
        List<String> skills = extractSkills(normalized);
        List<String> workTypes = extractWorkTypes(normalized);
        LocationGuess location = extractLocation(normalized);

        return new JobFilterRequest(
                new JobFilterRequest.JobCriteria(
                        null,
                        inferRole(normalized, skills),
                        null,
                        inferYears(normalized),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        inferExperienceLevel(normalized),
                        null
                ),
                new JobFilterRequest.LocationCriteria(
                    null,
                    location.city(),
                    location.city() == null ? List.of() : List.of(location.city()),
                    null,
                    List.of(),
                    location.country(),
                    location.country() == null ? List.of() : List.of(location.country()),
                    null,
                    null
                ),
                workTypes,
                skills
        );
    }

    private List<String> extractSkills(String normalized) {
        Map<String, String> allowedByName = allowedByName(allowedValuesService.getAllowedSkills());
        List<String> skills = new ArrayList<>();

        SKILL_ALIASES.forEach((canonical, aliases) -> {
            String allowed = allowedByName.get(normalize(canonical));
            if (allowed == null || skills.contains(allowed)) {
                return;
            }
            if (aliases.stream().map(this::normalize).anyMatch(normalized::contains)) {
                skills.add(allowed);
            }
        });

        allowedValuesService.getAllowedSkills().stream()
                .filter(skill -> normalized.contains(normalize(skill)))
                .filter(skill -> !skills.contains(skill))
                .forEach(skills::add);

        return skills;
    }

    private List<String> extractWorkTypes(String normalized) {
        Map<String, String> allowedByName = allowedByName(allowedValuesService.getAllowedWorkTypes());
        List<String> workTypes = new ArrayList<>();

        addAllowed(workTypes, allowedByName, "Remote", normalized.contains("remote")
                || normalized.contains("work from home")
                || normalized.contains("wfh")
                || normalized.contains("od kuce")
                || normalized.contains("na daljavo"));
        addAllowed(workTypes, allowedByName, "Hybrid", normalized.contains("hybrid") || normalized.contains("hibrid"));
        addAllowed(workTypes, allowedByName, "On-site", normalized.contains("on site")
                || normalized.contains("on-site")
                || normalized.contains("onsite"));

        return workTypes;
    }

    private LocationGuess extractLocation(String normalized) {
        String city = null;
        String country = null;

        for (Map.Entry<String, String> entry : CITY_ALIASES.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                city = entry.getValue();
                break;
            }
        }

        for (Map.Entry<String, String> entry : COUNTRY_ALIASES.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                country = entry.getValue();
                break;
            }
        }

        return new LocationGuess(city, country);
    }

    private String inferRole(String normalized, List<String> skills) {
        boolean dotnet = hasSkill(skills, ".NET") || hasSkill(skills, "C#") || normalized.contains("dotnet");
        boolean backend = normalized.contains("backend") || normalized.contains("back end") || normalized.contains("api");
        boolean frontend = normalized.contains("frontend") || normalized.contains("front end") || hasSkill(skills, "React") || hasSkill(skills, "Angular");
        boolean fullStack = normalized.contains("full stack") || normalized.contains("fullstack") || normalized.contains("full-stack");
        boolean java = hasSkill(skills, "Java") || hasSkill(skills, "Spring Boot");

        if (fullStack && dotnet) return "Full Stack .NET Developer";
        if (backend && dotnet) return ".NET Backend Developer";
        if (backend && java) return "Java Backend Developer";
        if (frontend && hasSkill(skills, "React")) return "React Frontend Developer";
        if (frontend && hasSkill(skills, "Angular")) return "Angular Frontend Developer";
        if (backend) return "Backend Developer";
        if (frontend) return "Frontend Developer";
        if (dotnet) return ".NET Developer";
        if (java) return "Java Developer";
        return null;
    }

    private Integer inferYears(String normalized) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(\\d+)\\s*(?:year|years|yr|yrs|leto|leta|godina|godine)")
                .matcher(normalized);
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private String inferExperienceLevel(String normalized) {
        if (normalized.contains("junior") || normalized.contains("entry")) return "Junior";
        if (normalized.contains("senior") || normalized.contains("lead")) return "Senior";
        if (normalized.contains("mid")) return "Mid";
        return null;
    }

    private boolean hasSkill(List<String> skills, String value) {
        String normalized = normalize(value);
        return skills.stream().map(this::normalize).anyMatch(skill -> skill.equals(normalized));
    }

    private void addAllowed(List<String> values, Map<String, String> allowedByName, String value, boolean shouldAdd) {
        String allowed = allowedByName.get(normalize(value));
        if (shouldAdd && allowed != null && !values.contains(allowed)) {
            values.add(allowed);
        }
    }

    private Map<String, String> allowedByName(List<String> values) {
        Map<String, String> result = new LinkedHashMap<>();
        values.forEach(value -> result.put(normalize(value), value));
        return result;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace("Ä", "c")
                .replace("Ä‡", "c")
                .replace("Å¡", "s")
                .replace("Å¾", "z")
                .replace("Ä‘", "dj")
                .replaceAll("[^a-z0-9+#.]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private record LocationGuess(String city, String country) {
    }
}
