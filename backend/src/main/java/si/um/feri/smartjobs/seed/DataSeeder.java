package si.um.feri.smartjobs.seed;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import si.um.feri.smartjobs.ai.service.AiAllowedValuesService;
import si.um.feri.smartjobs.job.service.JobService;
import si.um.feri.smartjobs.seed.educationLevel.EducationLevelSeed;
import si.um.feri.smartjobs.seed.experienceLevel.ExperienceLevelSeed;
import si.um.feri.smartjobs.seed.workType.WorkTypeSeed;
import si.um.feri.smartjobs.seed.location.LocationSeed;
import si.um.feri.smartjobs.seed.skillType.SkillTypeSeed;
import si.um.feri.smartjobs.seed.skill.SkillSeed;
import si.um.feri.smartjobs.seed.skillRelation.SkillRelationSeed;
import si.um.feri.smartjobs.seed.job.JobSeed;
import si.um.feri.smartjobs.seed.jobSkill.JobSkillSeed;
import si.um.feri.smartjobs.seed.workTypeJob.WorkTypeJobSeed;
import si.um.feri.smartjobs.seed.user.UserSeed;
import si.um.feri.smartjobs.seed.userSkill.UserSkillSeed;
import si.um.feri.smartjobs.seed.synthetic.SyntheticJobSeed;

@Component
public class DataSeeder implements CommandLineRunner {

    private final EducationLevelSeed educationLevelSeed;
    private final ExperienceLevelSeed experienceLevelSeed;
    private final WorkTypeSeed workTypeSeed;
    private final LocationSeed locationSeed;
    private final SkillTypeSeed skillTypeSeed;
    private final SkillSeed skillSeed;
    private final SkillRelationSeed skillRelationSeed;
    private final JobSeed jobSeed;
    private final JobSkillSeed jobSkillSeed;
    private final WorkTypeJobSeed workTypeJobSeed;
    private final UserSeed userSeed;
    private final UserSkillSeed userSkillSeed;
    private final SyntheticJobSeed sjs;
    private final AiAllowedValuesService aiAllowedValuesService;
    private final JobService jobService;

    public DataSeeder(
            EducationLevelSeed educationLevelSeed,
            ExperienceLevelSeed experienceLevelSeed,
            WorkTypeSeed workTypeSeed,
            LocationSeed locationSeed,
            SkillTypeSeed skillTypeSeed,
            SkillSeed skillSeed,
            SkillRelationSeed skillRelationSeed,
            JobSeed jobSeed,
            JobSkillSeed jobSkillSeed,
            WorkTypeJobSeed workTypeJobSeed,
            UserSeed userSeed,
            UserSkillSeed userSkillSeed,
            SyntheticJobSeed sjs,
            AiAllowedValuesService aiAllowedValuesService,
            JobService jobService) {
        this.educationLevelSeed = educationLevelSeed;
        this.experienceLevelSeed = experienceLevelSeed;
        this.workTypeSeed = workTypeSeed;
        this.locationSeed = locationSeed;
        this.skillTypeSeed = skillTypeSeed;
        this.skillSeed = skillSeed;
        this.skillRelationSeed = skillRelationSeed;
        this.jobSeed = jobSeed;
        this.jobSkillSeed = jobSkillSeed;
        this.workTypeJobSeed = workTypeJobSeed;
        this.userSeed = userSeed;
        this.userSkillSeed = userSkillSeed;
        this.sjs = sjs;
        this.aiAllowedValuesService = aiAllowedValuesService;
        this.jobService = jobService;
    }

    @Override
    public void run(String... args) {

        educationLevelSeed.seed();
        experienceLevelSeed.seed();
        workTypeSeed.seed();
        locationSeed.seed();
        skillTypeSeed.seed();

        skillSeed.seed();
        skillRelationSeed.seed();
        sjs.seedReferenceData();
        aiAllowedValuesService.refresh();
        jobService.refreshSkillRelationIndex();

        jobSeed.seed();

        userSeed.seed();

        jobSkillSeed.seed();
        workTypeJobSeed.seed();
        userSkillSeed.seed();
        sjs.seed();
        jobService.refreshJobLookupIndex();
    }
}
