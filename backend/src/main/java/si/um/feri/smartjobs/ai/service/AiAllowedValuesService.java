package si.um.feri.smartjobs.ai.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import si.um.feri.smartjobs.educationLevel.entity.EducationLevel;
import si.um.feri.smartjobs.educationLevel.repository.EducationLevelRepository;
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
    private final WorkTypeRepository workTypeRepository;

    private List<String> allowedSkills = List.of();
    private List<String> allowedEducationLevels = List.of();
    private List<String> allowedWorkTypes = List.of();

    public AiAllowedValuesService(
            SkillRepository skillRepository,
            EducationLevelRepository educationLevelRepository,
            WorkTypeRepository workTypeRepository
    ) {
        this.skillRepository = skillRepository;
        this.educationLevelRepository = educationLevelRepository;
        this.workTypeRepository = workTypeRepository;
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

        allowedEducationLevels = educationLevelRepository.findAll().stream()
                .map(EducationLevel::getName)
                .sorted()
                .toList();

        allowedWorkTypes = workTypeRepository.findAll().stream()
                .map(WorkType::getName)
                .sorted()
                .toList();

        LOGGER.info(
                "AI allowed values refreshed. Skills: {}, education levels: {}, work types: {}.",
                allowedSkills.size(),
                allowedEducationLevels.size(),
                allowedWorkTypes.size()
        );
    }

    public List<String> getAllowedSkills() {
        return allowedSkills;
    }

    public List<String> getAllowedEducationLevels() {
        return allowedEducationLevels;
    }

    public List<String> getAllowedWorkTypes() {
        return allowedWorkTypes;
    }
}
