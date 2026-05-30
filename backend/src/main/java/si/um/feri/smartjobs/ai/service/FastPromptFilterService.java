package si.um.feri.smartjobs.ai.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import si.um.feri.smartjobs.job.dto.JobFilterRequest;

@Service
public class FastPromptFilterService {

    private static final Map<String, String> COUNTRY_ALIASES = Map.ofEntries(
            Map.entry("slovenia", "Slovenia"),
            Map.entry("slovenija", "Slovenia"),
            Map.entry("slowenien", "Slovenia"),
            Map.entry("germany", "Germany"),
            Map.entry("nemcija", "Germany"),
            Map.entry("deutschland", "Germany"),
            Map.entry("austria", "Austria"),
            Map.entry("avstrija", "Austria"),
            Map.entry("osterreich", "Austria"),
            Map.entry("croatia", "Croatia"),
            Map.entry("hrvaska", "Croatia"),
            Map.entry("kroatien", "Croatia"),
            Map.entry("serbia", "Serbia"),
            Map.entry("srbija", "Serbia"),
            Map.entry("serbien", "Serbia")
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
            Map.entry("munchen", "Munich"),
            Map.entry("hamburg", "Hamburg"),
            Map.entry("vienna", "Vienna"),
            Map.entry("wien", "Vienna"),
            Map.entry("graz", "Graz"),
            Map.entry("linz", "Linz"),
            Map.entry("salzburg", "Salzburg")
    );

    private final AiAllowedValuesService allowedValuesService;
    private final SkillAliasMatcherService skillAliasMatcherService;

    public FastPromptFilterService(AiAllowedValuesService allowedValuesService) {
        this(allowedValuesService, new SkillAliasMatcherService());
    }

    @Autowired
    public FastPromptFilterService(
            AiAllowedValuesService allowedValuesService,
            SkillAliasMatcherService skillAliasMatcherService
    ) {
        this.allowedValuesService = allowedValuesService;
        this.skillAliasMatcherService = skillAliasMatcherService;
    }

    public JobFilterRequest buildFilter(String text) {
        String normalized = normalize(text);
        List<String> skills = extractSkills(normalized);
        List<String> workTypes = extractWorkTypes(normalized);
        LocationGuess location = extractLocation(normalized);

        return new JobFilterRequest(
                new JobFilterRequest.JobCriteria(
                        null,
                        null,
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
        return skillAliasMatcherService.extractAllowedSkills(normalized, allowedValuesService.getAllowedSkills());
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
