package si.um.feri.smartjobs.integration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import si.um.feri.smartjobs.ai.service.AiAllowedValuesService;
import si.um.feri.smartjobs.analytics.service.AnalyticsService;
import si.um.feri.smartjobs.educationLevel.entity.EducationLevel;
import si.um.feri.smartjobs.educationLevel.repository.EducationLevelRepository;
import si.um.feri.smartjobs.experienceLevel.entity.ExperienceLevel;
import si.um.feri.smartjobs.experienceLevel.repository.ExperienceLevelRepository;
import si.um.feri.smartjobs.job.entity.Job;
import si.um.feri.smartjobs.job.repository.JobRepository;
import si.um.feri.smartjobs.job.service.JobService;
import si.um.feri.smartjobs.jobSkill.entity.JobSkill;
import si.um.feri.smartjobs.jobSkill.repository.JobSkillRepository;
import si.um.feri.smartjobs.location.entity.Location;
import si.um.feri.smartjobs.location.repository.LocationRepository;
import si.um.feri.smartjobs.skill.entity.Skill;
import si.um.feri.smartjobs.skill.repository.SkillRepository;
import si.um.feri.smartjobs.skillRelation.entity.SkillRelation;
import si.um.feri.smartjobs.skillRelation.repository.SkillRelationRepository;
import si.um.feri.smartjobs.skillType.entity.SkillType;
import si.um.feri.smartjobs.skillType.repository.SkillTypeRepository;
import si.um.feri.smartjobs.workType.entity.WorkType;
import si.um.feri.smartjobs.workType.repository.WorkTypeRepository;
import si.um.feri.smartjobs.workTypeJob.entity.WorkTypeJob;
import si.um.feri.smartjobs.workTypeJob.repository.WorkTypeJobRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class AbstractIntegrationTestData {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;

    @Autowired protected EducationLevelRepository educationLevelRepository;
    @Autowired protected ExperienceLevelRepository experienceLevelRepository;
    @Autowired protected LocationRepository locationRepository;
    @Autowired protected SkillTypeRepository skillTypeRepository;
    @Autowired protected SkillRepository skillRepository;
    @Autowired protected SkillRelationRepository skillRelationRepository;
    @Autowired protected WorkTypeRepository workTypeRepository;
    @Autowired protected JobRepository jobRepository;
    @Autowired protected JobSkillRepository jobSkillRepository;
    @Autowired protected WorkTypeJobRepository workTypeJobRepository;
    @Autowired protected AiAllowedValuesService aiAllowedValuesService;
    @Autowired protected AnalyticsService analyticsService;
    @Autowired protected JobService jobService;

    protected EducationLevel bachelor;
    protected EducationLevel secondary;
    protected ExperienceLevel junior;
    protected ExperienceLevel mid;
    protected ExperienceLevel senior;
    protected Location vienna;
    protected Location graz;
    protected Location ljubljana;
    protected Skill mechatronics;
    protected Skill react;
    protected Skill nursing;
    protected Skill hotelService;
    protected Skill javaSkill;
    protected WorkType remote;
    protected WorkType hybrid;
    protected WorkType onsite;
    protected WorkType fieldWork;
    protected WorkType notSpecified;
    protected Job viennaReactJob;
    protected Job grazMechatronicsJob;
    protected Job ljubljanaNurseJob;
    protected Job viennaHotelJob;

    @BeforeEach
    void seedIntegrationData() {
        clearData();

        bachelor = educationLevelRepository.save(new EducationLevel("edu-bachelor", "Bachelor"));
        secondary = educationLevelRepository.save(new EducationLevel("edu-secondary", "Secondary"));

        junior = experienceLevelRepository.save(new ExperienceLevel("exp-junior", "Junior"));
        mid = experienceLevelRepository.save(new ExperienceLevel("exp-mid", "Mid"));
        senior = experienceLevelRepository.save(new ExperienceLevel("exp-senior", "Senior"));

        vienna = locationRepository.save(new Location("loc-vienna-austria", null, "Vienna", "Vienna", "Austria",
                new BigDecimal("48.2082"), new BigDecimal("16.3738")));
        graz = locationRepository.save(new Location("loc-graz-austria", null, "Graz", "Styria", "Austria",
                new BigDecimal("47.0707"), new BigDecimal("15.4395")));
        ljubljana = locationRepository.save(new Location("loc-ljubljana-slovenia", null, "Ljubljana", "Central Slovenia", "Slovenia",
                new BigDecimal("46.0569"), new BigDecimal("14.5058")));

        SkillType professional = skillTypeRepository.save(new SkillType("skill-type-professional", "Professional"));
        mechatronics = skillRepository.save(new Skill("skill-mechatronics", "Mechatronics", professional));
        react = skillRepository.save(new Skill("skill-react", "React", professional));
        nursing = skillRepository.save(new Skill("skill-nursing", "Nursing", professional));
        hotelService = skillRepository.save(new Skill("skill-hotel-service", "Hotel Service", professional));
        javaSkill = skillRepository.save(new Skill("skill-java", "Java", professional));

        skillRelationRepository.save(new SkillRelation("rel-react-java", "RELATED", react, javaSkill));

        remote = workTypeRepository.save(new WorkType("work-remote", "Remote"));
        hybrid = workTypeRepository.save(new WorkType("work-hybrid", "Hybrid"));
        onsite = workTypeRepository.save(new WorkType("work-on-site", "On-site"));
        fieldWork = workTypeRepository.save(new WorkType("work-field", "Field work"));
        notSpecified = workTypeRepository.save(new WorkType("work-not-specified", "Not specified"));

        LocalDate posted = LocalDate.of(2026, 5, 20);
        viennaReactJob = jobRepository.save(job(
                "job-vienna-react",
                "Austrian Tech",
                "React Developer",
                "React frontend role in Vienna with remote work.",
                3,
                new BigDecimal("3000"),
                new BigDecimal("4500"),
                vienna,
                senior,
                bachelor,
                posted
        ));
        grazMechatronicsJob = jobRepository.save(job(
                "job-graz-mechatronics",
                "Graz Robotics",
                "Mechatronics Technician",
                "Field work on robotics equipment in Graz.",
                2,
                new BigDecimal("2400"),
                new BigDecimal("3600"),
                graz,
                mid,
                secondary,
                posted.minusDays(1)
        ));
        ljubljanaNurseJob = jobRepository.save(job(
                "job-ljubljana-nurse",
                "Ljubljana Clinic",
                "Nurse",
                "Patient care and nursing role.",
                1,
                new BigDecimal("1800"),
                new BigDecimal("2600"),
                ljubljana,
                junior,
                bachelor,
                posted.minusDays(2)
        ));
        viennaHotelJob = jobRepository.save(job(
                "job-vienna-hotel",
                "Vienna Hotel",
                "Hotel Service Associate",
                "Hospitality service job without salary data.",
                null,
                null,
                null,
                vienna,
                junior,
                secondary,
                posted.minusDays(3)
        ));

        jobSkillRepository.saveAll(List.of(
                new JobSkill("job-skill-vienna-react", viennaReactJob, react),
                new JobSkill("job-skill-vienna-java", viennaReactJob, javaSkill),
                new JobSkill("job-skill-graz-mechatronics", grazMechatronicsJob, mechatronics),
                new JobSkill("job-skill-ljubljana-nursing", ljubljanaNurseJob, nursing),
                new JobSkill("job-skill-vienna-hotel", viennaHotelJob, hotelService)
        ));

        workTypeJobRepository.saveAll(List.of(
                new WorkTypeJob("work-job-vienna-react", viennaReactJob, remote),
                new WorkTypeJob("work-job-vienna-react-hybrid", viennaReactJob, hybrid),
                new WorkTypeJob("work-job-graz-mechatronics", grazMechatronicsJob, fieldWork),
                new WorkTypeJob("work-job-ljubljana-nurse", ljubljanaNurseJob, onsite),
                new WorkTypeJob("work-job-vienna-hotel", viennaHotelJob, onsite)
        ));

        aiAllowedValuesService.refresh();
        jobService.refreshSkillRelationIndex();
        jobService.refreshJobLookupIndex();
        analyticsService.refreshDashboardCache();
    }

    protected void clearData() {
        workTypeJobRepository.deleteAll();
        jobSkillRepository.deleteAll();
        skillRelationRepository.deleteAll();
        jobRepository.deleteAll();
        workTypeRepository.deleteAll();
        skillRepository.deleteAll();
        skillTypeRepository.deleteAll();
        locationRepository.deleteAll();
        experienceLevelRepository.deleteAll();
        educationLevelRepository.deleteAll();
    }

    private Job job(
            String id,
            String company,
            String title,
            String description,
            Integer requiredExperience,
            BigDecimal minSalary,
            BigDecimal maxSalary,
            Location location,
            ExperienceLevel experienceLevel,
            EducationLevel educationLevel,
            LocalDate posted
    ) {
        Job job = new Job(
                id,
                company,
                title,
                description,
                requiredExperience,
                null,
                null,
                "integration-test",
                posted,
                posted,
                posted,
                minSalary,
                maxSalary,
                experienceLevel,
                location,
                educationLevel
        );
        job.setStatus("ACTIVE");
        job.setSourceJobKey(id + "-source-key");
        return job;
    }
}
