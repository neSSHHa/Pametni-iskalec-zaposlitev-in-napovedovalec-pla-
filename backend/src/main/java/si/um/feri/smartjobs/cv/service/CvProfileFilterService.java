package si.um.feri.smartjobs.cv.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import si.um.feri.smartjobs.ai.service.AiAllowedValuesService;
import si.um.feri.smartjobs.ai.service.SkillAliasMatcherService;
import si.um.feri.smartjobs.job.dto.JobFilterRequest;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
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

    private final AiAllowedValuesService allowedValuesService;
    private final SkillAliasMatcherService skillAliasMatcherService;

    public CvProfileFilterService(AiAllowedValuesService allowedValuesService) {
        this(allowedValuesService, new SkillAliasMatcherService());
    }

    @Autowired
    public CvProfileFilterService(
            AiAllowedValuesService allowedValuesService,
            SkillAliasMatcherService skillAliasMatcherService
    ) {
        this.allowedValuesService = allowedValuesService;
        this.skillAliasMatcherService = skillAliasMatcherService;
    }

    public JobFilterRequest buildFilter(String cvText) {
        String normalizedCv = normalize(cvText);
        List<String> skills = extractSkills(normalizedCv);

        return new JobFilterRequest(
                new JobFilterRequest.JobCriteria(
                        null,
                        null,
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
        return skillAliasMatcherService.extractAllowedSkills(
                normalizedCv,
                allowedValuesService.getAllowedSkills(),
                SOFT_SKILLS
        );
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
