package si.um.feri.smartjobs.ai.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class SkillAliasMatcherService {

    private static final String ALIASES_RESOURCE = "/skill-aliases.csv";

    private final Map<String, List<String>> aliasesByCanonicalName;

    public SkillAliasMatcherService() {
        aliasesByCanonicalName = loadAliases();
    }

    public List<String> extractAllowedSkills(String text, List<String> allowedSkills) {
        return extractAllowedSkills(text, allowedSkills, Set.of());
    }

    public List<String> extractAllowedSkills(String text, List<String> allowedSkills, Set<String> excludedSkills) {
        String normalizedText = normalize(text);
        if (normalizedText.isEmpty() || allowedSkills == null || allowedSkills.isEmpty()) {
            return List.of();
        }

        Set<String> normalizedExcludedSkills = excludedSkills.stream()
                .map(this::normalize)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
        List<String> matches = new ArrayList<>();

        for (String allowedSkill : allowedSkills) {
            String normalizedSkill = normalize(allowedSkill);
            if (normalizedSkill.isEmpty() || normalizedExcludedSkills.contains(normalizedSkill)) {
                continue;
            }

            List<String> aliases = aliasesByCanonicalName.getOrDefault(normalizedSkill, List.of());
            if (containsTerm(normalizedText, normalizedSkill)
                    || aliases.stream().anyMatch(alias -> containsTerm(normalizedText, alias))) {
                matches.add(allowedSkill);
            }
        }

        return matches;
    }

    public String normalize(String value) {
        if (value == null) {
            return "";
        }

        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9+#.]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean containsTerm(String normalizedText, String normalizedTerm) {
        return !normalizedTerm.isEmpty()
                && Pattern.compile("(^|[^a-z0-9+#])" + Pattern.quote(normalizedTerm) + "($|[^a-z0-9+#])")
                        .matcher(normalizedText)
                        .find();
    }

    private Map<String, List<String>> loadAliases() {
        InputStream inputStream = SkillAliasMatcherService.class.getResourceAsStream(ALIASES_RESOURCE);
        if (inputStream == null) {
            throw new IllegalStateException("Missing skill alias resource: " + ALIASES_RESOURCE);
        }

        Map<String, LinkedHashSet<String>> aliases = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }

                String[] columns = line.split(";", 2);
                if (columns.length != 2) {
                    throw new IllegalStateException("Invalid skill alias row: " + line);
                }

                String canonicalName = normalize(columns[0]);
                LinkedHashSet<String> values = aliases.computeIfAbsent(canonicalName, ignored -> new LinkedHashSet<>());
                for (String alias : columns[1].split("\\|")) {
                    String normalizedAlias = normalize(alias);
                    if (!normalizedAlias.isEmpty()) {
                        values.add(normalizedAlias);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not load skill aliases.", e);
        }

        Map<String, List<String>> result = new LinkedHashMap<>();
        aliases.forEach((canonicalName, values) -> result.put(canonicalName, List.copyOf(values)));
        return Map.copyOf(result);
    }
}
