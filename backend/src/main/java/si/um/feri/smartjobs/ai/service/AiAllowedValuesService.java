package si.um.feri.smartjobs.ai.service;

import java.util.List;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import si.um.feri.smartjobs.educationLevel.entity.EducationLevel;
import si.um.feri.smartjobs.educationLevel.repository.EducationLevelRepository;
import si.um.feri.smartjobs.experienceLevel.entity.ExperienceLevel;
import si.um.feri.smartjobs.experienceLevel.repository.ExperienceLevelRepository;
import si.um.feri.smartjobs.location.entity.Location;
import si.um.feri.smartjobs.location.repository.LocationRepository;
import si.um.feri.smartjobs.skill.entity.Skill;
import si.um.feri.smartjobs.skill.repository.SkillRepository;
import si.um.feri.smartjobs.workType.entity.WorkType;
import si.um.feri.smartjobs.workType.repository.WorkTypeRepository;

/*
@PostConstruct pomeni: ko se aplikacija zažene, takoj napolni cache.
@Scheduled(cron = "0 0 3 * * MON") pomeni: vsak ponedeljek ob 03:00.
zone = "Europe/Ljubljana" pomeni, da uporablja naš lokalni čas.
refresh() bere iz baze in shrani v liste.


*/

@Service
public class AiAllowedValuesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiAllowedValuesService.class);

    private final SkillRepository skillRepository;
    private final EducationLevelRepository educationLevelRepository;
    private final ExperienceLevelRepository experienceLevelRepository;
    private final WorkTypeRepository workTypeRepository;
    private final LocationRepository locationRepository;

    private List<String> allowedSkills = List.of();
    private List<String> allowedEducationLevels = List.of();
    private List<String> allowedExperienceLevels = List.of();
    private List<String> allowedWorkTypes = List.of();
    private List<String> allowedLocations = List.of();
    private List<CachedSkill> cachedSkills = List.of();

    public AiAllowedValuesService(
            SkillRepository skillRepository,
            EducationLevelRepository educationLevelRepository,
            ExperienceLevelRepository experienceLevelRepository,
            WorkTypeRepository workTypeRepository,
            LocationRepository locationRepository
    ) {
        this.skillRepository = skillRepository;
        this.educationLevelRepository = educationLevelRepository;
        this.experienceLevelRepository = experienceLevelRepository;
        this.workTypeRepository = workTypeRepository;
        this.locationRepository = locationRepository;
    }

    @PostConstruct
    public void loadOnStartup() {
        refresh();
    }

    @Scheduled(cron = "0 0 3 * * MON", zone = "Europe/Ljubljana")
    public void refreshWeekly() {
        refresh();
    }

    public void refresh() {
        allowedSkills = skillRepository.findAll().stream()
                .map(Skill::getName)
                .sorted()
                .toList();
        cachedSkills = allowedSkills.stream()
                .map(skill -> new CachedSkill(skill, normalize(skill), significantTokens(skill)))
                .toList();

        allowedEducationLevels = educationLevelRepository.findAll().stream()
                .map(EducationLevel::getName)
                .sorted()
                .toList();

        allowedExperienceLevels = experienceLevelRepository.findAll().stream()
                .map(ExperienceLevel::getName)
                .sorted()
                .toList();

        allowedWorkTypes = workTypeRepository.findAll().stream()
                .map(WorkType::getName)
                .sorted()
                .toList();

        allowedLocations = locationRepository.findAll().stream()
                .<String>flatMap(location -> Arrays.stream(new String[]{
                        location.getCityDistrict(),
                        location.getCity(),
                        location.getRegion(),
                        location.getCountry()
                }))
                .filter(this::hasText)
                .sorted()
                .distinct()
                .toList();

        LOGGER.info(
                "AI allowed values refreshed. Skills: {}, education levels: {}, experience levels: {}, work types: {}, locations: {}.",
                allowedSkills.size(),
                allowedEducationLevels.size(),
                allowedExperienceLevels.size(),
                allowedWorkTypes.size(),
                allowedLocations.size()
        );
    }

    public List<String> getAllowedSkills() {
        return allowedSkills;
    }

    public List<String> getRelevantAllowedSkills(String text) {
        String normalizedText = normalize(text);
        Set<String> terms = significantTokens(normalizedText);

        List<String> matches = cachedSkills.stream()
                .filter(skill -> isRelevantSkill(skill, normalizedText, terms))
                .map(CachedSkill::name)
                .limit(180)
                .toList();

        if (matches.size() >= 8 || allowedSkills.size() <= 220) {
            return matches;
        }

        return allowedSkills.stream().limit(220).toList();
    }

    public List<String> getAllowedEducationLevels() {
        return allowedEducationLevels;
    }

    public List<String> getAllowedExperienceLevels() {
        return allowedExperienceLevels;
    }

    public List<String> getAllowedWorkTypes() {
        return allowedWorkTypes;
    }

    public List<String> getAllowedLocations() {
        return allowedLocations;
    }

    private boolean isRelevantSkill(CachedSkill skill, String normalizedText, Set<String> terms) {
        String normalizedSkill = skill.normalizedName();
        if (!hasText(normalizedSkill)) {
            return false;
        }
        if (normalizedText.contains(normalizedSkill)) {
            return true;
        }

        Set<String> skillTokens = skill.tokens();
        if (skillTokens.isEmpty()) {
            return false;
        }

        long overlap = skillTokens.stream().filter(terms::contains).count();
        return skillTokens.size() <= 2 ? overlap == skillTokens.size() : overlap >= 2;
    }

    private Set<String> significantTokens(String value) {
        if (!hasText(value)) {
            return Set.of();
        }

        return Arrays.stream(normalize(value).split(" "))
                .filter(this::hasText)
                .filter(token -> token.length() > 1)
                .filter(token -> !Set.of("and", "or", "the", "with", "for", "job", "work", "role", "i", "am").contains(token))
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
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
                .replaceAll("[^a-z0-9+#.]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private record CachedSkill(String name, String normalizedName, Set<String> tokens) {
    }
}
