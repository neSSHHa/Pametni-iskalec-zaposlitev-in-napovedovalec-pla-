package si.um.feri.smartjobs.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import si.um.feri.smartjobs.educationLevel.entity.EducationLevel;
import si.um.feri.smartjobs.experienceLevel.entity.ExperienceLevel;
import si.um.feri.smartjobs.job.dto.JobDto;
import si.um.feri.smartjobs.job.dto.JobFilterRequest;
import si.um.feri.smartjobs.job.dto.JobSearchResponse;
import si.um.feri.smartjobs.job.entity.Job;
import si.um.feri.smartjobs.job.repository.JobRepository;
import si.um.feri.smartjobs.job.service.JobService;
import si.um.feri.smartjobs.jobSkill.entity.JobSkill;
import si.um.feri.smartjobs.jobSkill.repository.JobSkillRepository;
import si.um.feri.smartjobs.location.entity.Location;
import si.um.feri.smartjobs.skill.entity.Skill;
import si.um.feri.smartjobs.skillRelation.entity.SkillRelation;
import si.um.feri.smartjobs.skillRelation.repository.SkillRelationRepository;
import si.um.feri.smartjobs.workType.entity.WorkType;
import si.um.feri.smartjobs.workTypeJob.entity.WorkTypeJob;
import si.um.feri.smartjobs.workTypeJob.repository.WorkTypeJobRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobSkillRepository jobSkillRepository;

    @Mock
    private SkillRelationRepository skillRelationRepository;

    @Mock
    private WorkTypeJobRepository workTypeJobRepository;

    @InjectMocks
    private JobService jobService;

    @Test
    void shouldIncludeWorkModesInDto() {
        Job job = new Job(
                "job-1",
                "Smart School",
                "Biology Teacher",
                "Teach biology.",
                null,
                null,
                null,
                "https://example.com/job-1",
                LocalDate.of(2026, 5, 20),
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        WorkType onSite = new WorkType("work-on-site", "On-site");
        WorkTypeJob workTypeJob = new WorkTypeJob("wtj-1", job, onSite);

        when(jobRepository.findAll()).thenReturn(List.of(job));
        when(jobSkillRepository.findByJob_IdIn(List.of("job-1"))).thenReturn(List.of());
        when(workTypeJobRepository.findByJob_IdIn(List.of("job-1"))).thenReturn(List.of(workTypeJob));

        List<JobDto> result = jobService.findAll();

        assertEquals(1, result.size());
        assertEquals("On-site", result.get(0).workMode());
    }

    @Test
    void shouldReturnUnknownWhenJobHasNoWorkType() {
        Job job = new Job(
                "job-1",
                "Smart School",
                "Biology Teacher",
                "Teach biology.",
                null,
                null,
                null,
                "https://example.com/job-1",
                LocalDate.of(2026, 5, 20),
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(jobRepository.findAll()).thenReturn(List.of(job));
        when(jobSkillRepository.findByJob_IdIn(List.of("job-1"))).thenReturn(List.of());
        when(workTypeJobRepository.findByJob_IdIn(List.of("job-1"))).thenReturn(List.of());

        List<JobDto> result = jobService.findAll();

        assertEquals(1, result.size());
        assertEquals("Unknown", result.get(0).workMode());
    }

    @Test
    void shouldIncludeMultipleWorkModesInDto() {
        Job job = new Job(
                "job-1",
                "Smart School",
                "Biology Teacher",
                "Teach biology.",
                null,
                null,
                null,
                "https://example.com/job-1",
                LocalDate.of(2026, 5, 20),
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        WorkType remote = new WorkType("work-remote", "Remote");
        WorkType hybrid = new WorkType("work-hybrid", "Hybrid");
        WorkTypeJob remoteJob = new WorkTypeJob("wtj-1", job, remote);
        WorkTypeJob hybridJob = new WorkTypeJob("wtj-2", job, hybrid);

        when(jobRepository.findAll()).thenReturn(List.of(job));
        when(jobSkillRepository.findByJob_IdIn(List.of("job-1"))).thenReturn(List.of());
        when(workTypeJobRepository.findByJob_IdIn(List.of("job-1"))).thenReturn(List.of(remoteJob, hybridJob));

        List<JobDto> result = jobService.findAll();

        assertEquals(1, result.size());
        assertEquals("Remote, Hybrid", result.get(0).workMode());
    }

    @Test
    void shouldReturnEmptySkillsWhenJobHasNoSkills() {
        Job job = new Job(
                "job-1",
                "Smart School",
                "Biology Teacher",
                "Teach biology.",
                null,
                null,
                null,
                "https://example.com/job-1",
                LocalDate.of(2026, 5, 20),
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(jobRepository.findAll()).thenReturn(List.of(job));
        when(jobSkillRepository.findByJob_IdIn(List.of("job-1"))).thenReturn(List.of());
        when(workTypeJobRepository.findByJob_IdIn(List.of("job-1"))).thenReturn(List.of());

        List<JobDto> result = jobService.findAll();

        assertEquals(1, result.size());
        assertEquals(List.of(), result.get(0).skills());
    }

    @Test
    void shouldIncludeSkillInDto() {
        Job job = new Job(
                "job-1",
                "Smart School",
                "Biology Teacher",
                "Teach biology.",
                null,
                null,
                null,
                "https://example.com/job-1",
                LocalDate.of(2026, 5, 20),
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        Skill biology = new Skill("skill-biology", "Biology", null);
        JobSkill biologyJobSkill = new JobSkill("js-1", job, biology);

        when(jobRepository.findAll()).thenReturn(List.of(job));
        when(jobSkillRepository.findByJob_IdIn(List.of("job-1"))).thenReturn(List.of(biologyJobSkill));
        when(workTypeJobRepository.findByJob_IdIn(List.of("job-1"))).thenReturn(List.of());

        List<JobDto> result = jobService.findAll();

        assertEquals(1, result.size());
        assertEquals(List.of("Biology"), result.get(0).skills());
    }

    @Test
    void shouldIncludeMultipleSkillsInDto() {
        Job job = new Job(
                "job-1",
                "Smart School",
                "Biology Teacher",
                "Teach biology.",
                null,
                null,
                null,
                "https://example.com/job-1",
                LocalDate.of(2026, 5, 20),
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        Skill biology = new Skill("skill-biology", "Biology", null);
        Skill teaching = new Skill("skill-teaching", "Teaching", null);
        JobSkill biologyJobSkill = new JobSkill("js-1", job, biology);
        JobSkill teachingJobSkill = new JobSkill("js-2", job, teaching);

        when(jobRepository.findAll()).thenReturn(List.of(job));
        when(jobSkillRepository.findByJob_IdIn(List.of("job-1"))).thenReturn(List.of(biologyJobSkill, teachingJobSkill));
        when(workTypeJobRepository.findByJob_IdIn(List.of("job-1"))).thenReturn(List.of());

        List<JobDto> result = jobService.findAll();

        assertEquals(1, result.size());
        assertEquals(List.of("Biology", "Teaching"), result.get(0).skills());
    }

    @Test
    void shouldIncludeLocationInDto() {
        Location maribor = new Location(
                "loc-maribor-drava",
                null,
                "Maribor",
                "Drava",
                "Slovenia",
                BigDecimal.valueOf(46.55),
                BigDecimal.valueOf(15.65)
        );
        Job job = new Job(
                "job-1",
                "Smart School",
                "Biology Teacher",
                "Teach biology.",
                null,
                null,
                null,
                "https://example.com/job-1",
                LocalDate.of(2026, 5, 20),
                null,
                null,
                null,
                null,
                null,
                maribor,
                null
        );

        when(jobRepository.findAll()).thenReturn(List.of(job));
        when(jobSkillRepository.findByJob_IdIn(List.of("job-1"))).thenReturn(List.of());
        when(workTypeJobRepository.findByJob_IdIn(List.of("job-1"))).thenReturn(List.of());

        List<JobDto> result = jobService.findAll();

        assertEquals(1, result.size());
        assertEquals("Maribor, Drava, Slovenia", result.get(0).location());
        assertEquals("Maribor", result.get(0).city());
        assertEquals("Drava", result.get(0).region());
        assertEquals("Slovenia", result.get(0).country());
        assertEquals(BigDecimal.valueOf(46.55), result.get(0).latitude());
        assertEquals(BigDecimal.valueOf(15.65), result.get(0).longitude());
    }

   @Test
void shouldReturnNullLocationFieldsWhenJobHasNoLocation() {
    Job job = new Job(
            "job-1",
            "Smart School",
            "Biology Teacher",
            "Teach biology.",
            null,
            null,
            null,
            "https://example.com/job-1",
            LocalDate.of(2026, 5, 20),
            null,
            null,
            null,
            null,
            null,
            null,
            null
    );

    when(jobRepository.findAll()).thenReturn(List.of(job));
    when(jobSkillRepository.findByJob_IdIn(List.of("job-1"))).thenReturn(List.of());
    when(workTypeJobRepository.findByJob_IdIn(List.of("job-1"))).thenReturn(List.of());

    List<JobDto> result = jobService.findAll();

    assertEquals(1, result.size());
    assertEquals("Unknown", result.get(0).location());
    assertNull(result.get(0).city());
    assertNull(result.get(0).region());
    assertNull(result.get(0).country());
        assertNull(result.get(0).latitude());
        assertNull(result.get(0).longitude());
    }

    @Test
    void shouldIncludeExperienceLevelInDto() {
        ExperienceLevel mid = new ExperienceLevel("exp-mid", "Mid");
        Job job = new Job(
                "job-1",
                "Smart School",
                "Biology Teacher",
                "Teach biology.",
                null,
                null,
                null,
                "https://example.com/job-1",
                LocalDate.of(2026, 5, 20),
                null,
                null,
                null,
                null,
                mid,
                null,
                null
        );

        when(jobRepository.findAll()).thenReturn(List.of(job));
        when(jobSkillRepository.findByJob_IdIn(List.of("job-1"))).thenReturn(List.of());
        when(workTypeJobRepository.findByJob_IdIn(List.of("job-1"))).thenReturn(List.of());

        List<JobDto> result = jobService.findAll();

        assertEquals(1, result.size());
        assertEquals("Mid", result.get(0).experienceLevel());
    }

    @Test
    void shouldReturnUnknownWhenJobHasNoExperienceLevel() {
        Job job = new Job(
                "job-1",
                "Smart School",
                "Biology Teacher",
                "Teach biology.",
                null,
                null,
                null,
                "https://example.com/job-1",
                LocalDate.of(2026, 5, 20),
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(jobRepository.findAll()).thenReturn(List.of(job));
        when(jobSkillRepository.findByJob_IdIn(List.of("job-1"))).thenReturn(List.of());
        when(workTypeJobRepository.findByJob_IdIn(List.of("job-1"))).thenReturn(List.of());

        List<JobDto> result = jobService.findAll();

        assertEquals(1, result.size());
        assertEquals("Unknown", result.get(0).experienceLevel());
    }

    @Test
    void shouldIncludeEducationLevelInDto() {
        EducationLevel bachelor = new EducationLevel("edu-bachelor", "Bachelor");
        Job job = new Job(
                "job-1",
                "Smart School",
                "Biology Teacher",
                "Teach biology.",
                null,
                null,
                null,
                "https://example.com/job-1",
                LocalDate.of(2026, 5, 20),
                null,
                null,
                null,
                null,
                null,
                null,
                bachelor
        );

        when(jobRepository.findAll()).thenReturn(List.of(job));
        when(jobSkillRepository.findByJob_IdIn(List.of("job-1"))).thenReturn(List.of());
        when(workTypeJobRepository.findByJob_IdIn(List.of("job-1"))).thenReturn(List.of());

        List<JobDto> result = jobService.findAll();

        assertEquals(1, result.size());
        assertEquals("Bachelor", result.get(0).educationLevel());
    }

    @Test
    void shouldReturnUnknownWhenJobHasNoEducationLevel() {
        Job job = new Job(
                "job-1",
                "Smart School",
                "Biology Teacher",
                "Teach biology.",
                null,
                null,
                null,
                "https://example.com/job-1",
                LocalDate.of(2026, 5, 20),
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(jobRepository.findAll()).thenReturn(List.of(job));
        when(jobSkillRepository.findByJob_IdIn(List.of("job-1"))).thenReturn(List.of());
        when(workTypeJobRepository.findByJob_IdIn(List.of("job-1"))).thenReturn(List.of());

        List<JobDto> result = jobService.findAll();

        assertEquals(1, result.size());
        assertEquals("Unknown", result.get(0).educationLevel());
    }

    @Test
    void shouldReturnJobsWithoutFilters() {
        Job first = job("job-1", "Biology Teacher");
        Job second = job("job-2", "Java Developer");
        List<Job> jobs = List.of(first, second);

        when(jobRepository.findAll()).thenReturn(jobs);
        stubLookup(jobs, List.of(), List.of());

        JobSearchResponse response = jobService.filterResponse(emptyRequest(), 0, 10);

        assertEquals(2, response.jobs().size());
        assertEquals(List.of("job-1", "job-2"), response.jobs().stream().map(JobDto::id).toList());
    }

    @Test
    void shouldReturnFilteredAnalyticsForAllMatchesWhenResultsAreLimited() {
        List<Job> jobs = IntStream.rangeClosed(1, 201)
                .mapToObj(index -> job("job-" + index, "React Developer " + index))
                .toList();

        when(jobRepository.findAll()).thenReturn(jobs);
        stubLookup(jobs, List.of(), List.of());

        JobSearchResponse response = jobService.filterResponse(emptyRequest());

        assertEquals(JobService.DEFAULT_MATCH_LIMIT, response.jobs().size());
        assertEquals(201, response.totalCount());
        assertTrue(response.hasMore());
        assertEquals(201, response.analytics().summary().totalJobs());
    }

    @Test
    void shouldReturnJobsFromRequestedCountry() {
        Location slovenia = location("loc-maribor", "Maribor", "Drava", "Slovenia");
        Job slovenianJob = job("job-1", "Biology Teacher", slovenia);

        when(jobRepository.findByLocation_CountryContainingIgnoreCase("Slovenia")).thenReturn(List.of(slovenianJob));
        when(jobRepository.findByIdIn(anyCollection())).thenReturn(List.of(slovenianJob));
        stubLookup(List.of(slovenianJob), List.of(), List.of());

        JobSearchResponse response = jobService.filterResponse(countryRequest("Slovenia"), 0, 10);

        assertEquals(1, response.jobs().size());
        assertEquals("job-1", response.jobs().get(0).id());
    }

    @Test
    void shouldReturnJobsFromAustria() {
        Location austria = location("loc-vienna", "Vienna", "Vienna", "Austria");
        Job austrianJob = job("job-1", "Hotel Receptionist", austria);

        when(jobRepository.findByLocation_CountryContainingIgnoreCase("Austria")).thenReturn(List.of(austrianJob));
        when(jobRepository.findByIdIn(anyCollection())).thenReturn(List.of(austrianJob));
        stubLookup(List.of(austrianJob), List.of(), List.of());

        JobSearchResponse response = jobService.filterResponse(countryRequest("Austria"), 0, 10);

        assertEquals(1, response.jobs().size());
        assertEquals("job-1", response.jobs().get(0).id());
        assertEquals("Austria", response.jobs().get(0).country());
    }

    @Test
    void shouldReturnJobsFromRequestedCity() {
        Location wien = location("loc-wien", "Wien", "Vienna", "Austria");
        Job wienJob = job("job-1", "Biology Teacher", wien);

        when(jobRepository.findByLocation_CityContainingIgnoreCase("Wien")).thenReturn(List.of(wienJob));
        when(jobRepository.findByIdIn(anyCollection())).thenReturn(List.of(wienJob));
        stubLookup(List.of(wienJob), List.of(), List.of());

        JobSearchResponse response = jobService.filterResponse(cityRequest("Wien"), 0, 10);

        assertEquals(1, response.jobs().size());
        assertEquals("job-1", response.jobs().get(0).id());
    }

    @Test
    void shouldRankJobsWithRequestedSkillHigher() {
        Job biologyJob = job("job-1", "Teacher");
        Job chemistryJob = job("job-2", "Teacher");
        Skill biology = new Skill("skill-biology", "Biology", null);
        Skill chemistry = new Skill("skill-chemistry", "Chemistry", null);
        WorkType remote = new WorkType("work-remote", "Remote");
        JobSkill biologySkill = new JobSkill("js-1", biologyJob, biology);
        JobSkill chemistrySkill = new JobSkill("js-2", chemistryJob, chemistry);
        WorkTypeJob biologyRemote = new WorkTypeJob("wtj-1", biologyJob, remote);
        WorkTypeJob chemistryRemote = new WorkTypeJob("wtj-2", chemistryJob, remote);

        when(jobSkillRepository.findBySkill_NameIn(anyCollection())).thenReturn(List.of(biologySkill));
        when(jobSkillRepository.findBySkill_IdIn(anyCollection())).thenReturn(List.of());
        when(workTypeJobRepository.findByWorkType_NameIn(anyCollection())).thenReturn(List.of(biologyRemote, chemistryRemote));
        when(jobRepository.findByIdIn(anyCollection())).thenReturn(List.of(biologyJob, chemistryJob));
        stubLookup(List.of(biologyJob, chemistryJob), List.of(biologySkill, chemistrySkill), List.of(biologyRemote, chemistryRemote));

        JobSearchResponse response = jobService.filterResponse(skillAndWorkTypeRequest("Biology", "Remote"), 0, 10);

        assertEquals(2, response.jobs().size());
        assertEquals("job-1", response.jobs().get(0).id());
        assertTrue(response.jobs().get(0).matchScore() > response.jobs().get(1).matchScore());
    }

    @Test
    void shouldRankJobWithMultipleRequestedSkillsHigher() {
        Job fullMatchJob = job("job-1", "Science Teacher");
        Job partialMatchJob = job("job-2", "Science Teacher");
        Skill biology = new Skill("skill-biology", "Biology", null);
        Skill chemistry = new Skill("skill-chemistry", "Chemistry", null);
        JobSkill fullBiology = new JobSkill("js-1", fullMatchJob, biology);
        JobSkill fullChemistry = new JobSkill("js-2", fullMatchJob, chemistry);
        JobSkill partialBiology = new JobSkill("js-3", partialMatchJob, biology);

        when(jobSkillRepository.findBySkill_NameIn(anyCollection()))
                .thenReturn(List.of(fullBiology, fullChemistry, partialBiology));
        when(jobSkillRepository.findBySkill_IdIn(anyCollection())).thenReturn(List.of());
        when(jobRepository.findByIdIn(anyCollection())).thenReturn(List.of(fullMatchJob, partialMatchJob));
        stubLookup(
                List.of(fullMatchJob, partialMatchJob),
                List.of(fullBiology, fullChemistry, partialBiology),
                List.of()
        );

        JobSearchResponse response = jobService.filterResponse(skillsRequest("Biology", "Chemistry"), 0, 10);

        assertEquals(2, response.jobs().size());
        assertEquals("job-1", response.jobs().get(0).id());
        assertTrue(response.jobs().get(0).matchScore() > response.jobs().get(1).matchScore());
    }

    @Test
    void shouldRankJobsWithRequestedWorkTypeHigher() {
        Job remoteJob = job("job-1", "Teacher");
        Job onsiteJob = job("job-2", "Teacher");
        Skill biology = new Skill("skill-biology", "Biology", null);
        WorkType remote = new WorkType("work-remote", "Remote");
        WorkType onsite = new WorkType("work-on-site", "On-site");
        JobSkill remoteSkill = new JobSkill("js-1", remoteJob, biology);
        JobSkill onsiteSkill = new JobSkill("js-2", onsiteJob, biology);
        WorkTypeJob remoteWorkType = new WorkTypeJob("wtj-1", remoteJob, remote);
        WorkTypeJob onsiteWorkType = new WorkTypeJob("wtj-2", onsiteJob, onsite);

        when(jobSkillRepository.findBySkill_NameIn(anyCollection())).thenReturn(List.of(remoteSkill, onsiteSkill));
        when(jobSkillRepository.findBySkill_IdIn(anyCollection())).thenReturn(List.of());
        when(workTypeJobRepository.findByWorkType_NameIn(anyCollection())).thenReturn(List.of(remoteWorkType));
        when(jobRepository.findByIdIn(anyCollection())).thenReturn(List.of(remoteJob, onsiteJob));
        stubLookup(List.of(remoteJob, onsiteJob), List.of(remoteSkill, onsiteSkill), List.of(remoteWorkType, onsiteWorkType));

        JobSearchResponse response = jobService.filterResponse(skillAndWorkTypeRequest("Biology", "Remote"), 0, 10);

        assertEquals(2, response.jobs().size());
        assertEquals("job-1", response.jobs().get(0).id());
        assertTrue(response.jobs().get(0).matchScore() > response.jobs().get(1).matchScore());
    }

    @Test
    void shouldRankRelatedSkillJobAsMatch() {
        Job springJob = job("job-1", "Backend Developer");
        Skill java = new Skill("skill-java", "Java", null);
        Skill springBoot = new Skill("skill-spring-boot", "Spring Boot", null);
        SkillRelation springFrameworkOfJava = new SkillRelation("relation-1", "FRAMEWORK_OF", springBoot, java);
        JobSkill springSkill = new JobSkill("js-1", springJob, springBoot);

        when(skillRelationRepository.findAll()).thenReturn(List.of(springFrameworkOfJava));
        jobService.refreshSkillRelationIndex();
        when(jobSkillRepository.findBySkill_NameIn(anyCollection())).thenReturn(List.of(springSkill));
        when(jobSkillRepository.findBySkill_IdIn(anyCollection())).thenReturn(List.of());
        when(jobRepository.findByIdIn(anyCollection())).thenReturn(List.of(springJob));
        stubLookup(List.of(springJob), List.of(springSkill), List.of());

        JobSearchResponse response = jobService.filterResponse(skillsRequest("Java"), 0, 10);

        assertEquals(1, response.jobs().size());
        assertEquals("job-1", response.jobs().get(0).id());
        assertTrue(response.jobs().get(0).matchScore() > 0);
    }

    @Test
    void shouldReturnEmptyListWhenLocationHasNoCandidates() {
        when(jobRepository.findByLocation_CountryContainingIgnoreCase("Austria")).thenReturn(List.of());

        JobSearchResponse response = jobService.filterResponse(countryRequest("Austria"), 0, 10);

        assertEquals(List.of(), response.jobs());
        assertEquals(0, response.totalCount());
    }

    @Test
    void shouldRankJobsWithMatchingRequiredExperienceHigher() {
        Job matchingJob = jobWithRequiredExperience("job-1", 2);
        Job moreExperiencedJob = jobWithRequiredExperience("job-2", 5);
        List<Job> jobs = List.of(matchingJob, moreExperiencedJob);

        when(jobRepository.findAll()).thenReturn(jobs);
        stubLookup(jobs, List.of(), List.of());

        JobSearchResponse response = jobService.filterResponse(requiredExperienceRequest(2), 0, 10);

        assertEquals(2, response.jobs().size());
        assertEquals("job-1", response.jobs().get(0).id());
        assertTrue(response.jobs().get(0).matchScore() > response.jobs().get(1).matchScore());
    }

    @Test
    void shouldRankJobsWithRequestedEducationHigher() {
        EducationLevel bachelor = new EducationLevel("edu-bachelor", "Bachelor");
        EducationLevel secondary = new EducationLevel("edu-secondary", "Secondary");
        Job bachelorJob = jobWithEducation("job-1", bachelor);
        Job secondaryJob = jobWithEducation("job-2", secondary);
        List<Job> jobs = List.of(bachelorJob, secondaryJob);

        when(jobRepository.findAll()).thenReturn(jobs);
        stubLookup(jobs, List.of(), List.of());

        JobSearchResponse response = jobService.filterResponse(educationRequest("Bachelor"), 0, 10);

        assertEquals(2, response.jobs().size());
        assertEquals("job-1", response.jobs().get(0).id());
        assertTrue(response.jobs().get(0).matchScore() > response.jobs().get(1).matchScore());
    }

    @Test
    void shouldRankJobsWithinSalaryRangeHigher() {
        Job matchingSalaryJob = jobWithSalary("job-1", BigDecimal.valueOf(2000), BigDecimal.valueOf(3000));
        Job lowSalaryJob = jobWithSalary("job-2", BigDecimal.valueOf(900), BigDecimal.valueOf(1200));
        List<Job> jobs = List.of(matchingSalaryJob, lowSalaryJob);

        when(jobRepository.findAll()).thenReturn(jobs);
        stubLookup(jobs, List.of(), List.of());

        JobSearchResponse response = jobService.filterResponse(salaryRequest(BigDecimal.valueOf(1800), BigDecimal.valueOf(3200)), 0, 10);

        assertEquals(2, response.jobs().size());
        assertEquals("job-1", response.jobs().get(0).id());
        assertTrue(response.jobs().get(0).matchScore() > response.jobs().get(1).matchScore());
    }

    @Test
    void shouldSearchJobsByQuery() {
        Job biologyTeacher = job("job-1", "Biology Teacher");
        List<Job> jobs = List.of(biologyTeacher);

        when(jobRepository.findByJobNameContainingIgnoreCaseOrCompanyNameContainingIgnoreCase("biology", "biology"))
                .thenReturn(jobs);
        stubLookup(jobs, List.of(), List.of());

        List<JobDto> result = jobService.search("biology");

        assertEquals(1, result.size());
        assertEquals("job-1", result.get(0).id());
        assertEquals("Biology Teacher", result.get(0).title());
    }

    @Test
    void shouldReturnAllJobsWhenSearchQueryIsBlank() {
        Job first = job("job-1", "Biology Teacher");
        Job second = job("job-2", "Chemistry Teacher");
        List<Job> jobs = List.of(first, second);

        when(jobRepository.findAll()).thenReturn(jobs);
        stubLookup(jobs, List.of(), List.of());

        List<JobDto> result = jobService.search(" ");

        assertEquals(2, result.size());
        assertEquals(List.of("job-1", "job-2"), result.stream().map(JobDto::id).toList());
        verify(jobRepository, never()).findByJobNameContainingIgnoreCaseOrCompanyNameContainingIgnoreCase(" ", " ");
    }

    @Test
    void shouldReturnEmptyListWhenSearchHasNoMatches() {
        when(jobRepository.findByJobNameContainingIgnoreCaseOrCompanyNameContainingIgnoreCase("biology", "biology"))
                .thenReturn(List.of());

        List<JobDto> result = jobService.search("biology");

        assertEquals(List.of(), result);
    }

    @Test
    void shouldReturnPagedJobsWhenFilterRequestIsNull() {
        Job first = job("job-1", "Biology Teacher");
        Job second = job("job-2", "Chemistry Teacher");
        List<Job> pageJobs = List.of(first, second);

        when(jobRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(pageJobs, PageRequest.of(0, 10), 2));
        stubLookup(pageJobs, List.of(), List.of());

        JobSearchResponse response = jobService.filterResponse(null, 0, 10);

        assertEquals(2, response.jobs().size());
        assertEquals(2, response.totalCount());
        assertEquals(0, response.page());
        assertEquals(10, response.size());
        assertEquals(false, response.hasMore());
        assertEquals(List.of("job-1", "job-2"), response.jobs().stream().map(JobDto::id).toList());
    }

    @Test
    void shouldPaginateResults() {
        Job first = job("job-1", "Biology Teacher");
        Job second = job("job-2", "Chemistry Teacher");
        List<Job> pageJobs = List.of(first, second);

        when(jobRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(pageJobs, PageRequest.of(0, 2), 5));
        stubLookup(pageJobs, List.of(), List.of());

        JobSearchResponse response = jobService.findAllPage(0, 2);

        assertEquals(2, response.jobs().size());
        assertEquals(5, response.totalCount());
        assertEquals(0, response.page());
        assertEquals(2, response.size());
        assertTrue(response.hasMore());
        assertEquals(List.of("job-1", "job-2"), response.jobs().stream().map(JobDto::id).toList());
    }

    @Test
    void shouldReturnHasMoreFalseOnLastPage() {
        Job first = job("job-1", "Biology Teacher");
        Job second = job("job-2", "Chemistry Teacher");
        List<Job> pageJobs = List.of(first, second);

        when(jobRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(pageJobs, PageRequest.of(0, 2), 2));
        stubLookup(pageJobs, List.of(), List.of());

        JobSearchResponse response = jobService.findAllPage(0, 2);

        assertEquals(2, response.jobs().size());
        assertEquals(2, response.totalCount());
        assertEquals(0, response.page());
        assertEquals(2, response.size());
        assertEquals(false, response.hasMore());
    }

    @Test
    void shouldClampInvalidPaginationValues() {
        List<Job> pageJobs = List.of();

        when(jobRepository.findAll(PageRequest.of(0, JobService.MAX_PAGE_SIZE)))
                .thenReturn(new PageImpl<>(pageJobs, PageRequest.of(0, JobService.MAX_PAGE_SIZE), 0));

        JobSearchResponse response = jobService.findAllPage(-5, 9999);

        assertEquals(0, response.jobs().size());
        assertEquals(0, response.totalCount());
        assertEquals(0, response.page());
        assertEquals(JobService.MAX_PAGE_SIZE, response.size());
        assertEquals(false, response.hasMore());
    }

    private JobFilterRequest emptyRequest() {
        return new JobFilterRequest(null, null, List.of(), List.of());
    }

    private JobFilterRequest countryRequest(String country) {
        return new JobFilterRequest(
                null,
                new JobFilterRequest.LocationCriteria(null, null, List.of(), null, List.of(), country, List.of(), null, null),
                List.of(),
                List.of()
        );
    }

    private JobFilterRequest cityRequest(String city) {
        return new JobFilterRequest(
                null,
                new JobFilterRequest.LocationCriteria(null, city, List.of(), null, List.of(), null, List.of(), null, null),
                List.of(),
                List.of()
        );
    }

    private JobFilterRequest skillAndWorkTypeRequest(String skill, String workType) {
        return new JobFilterRequest(null, null, List.of(workType), List.of(skill));
    }

    private JobFilterRequest skillsRequest(String... skills) {
        return new JobFilterRequest(null, null, List.of(), List.of(skills));
    }

    private JobFilterRequest requiredExperienceRequest(Integer requiredExperience) {
        return new JobFilterRequest(
                new JobFilterRequest.JobCriteria(null, "Teacher", null, requiredExperience, null, null, null, null, null, null, null, null),
                null,
                List.of(),
                List.of()
        );
    }

    private JobFilterRequest educationRequest(String education) {
        return new JobFilterRequest(
                new JobFilterRequest.JobCriteria(null, "Teacher", null, null, null, null, null, null, null, null, null, education),
                null,
                List.of(),
                List.of()
        );
    }

    private JobFilterRequest salaryRequest(BigDecimal minSalary, BigDecimal maxSalary) {
        return new JobFilterRequest(
                new JobFilterRequest.JobCriteria(null, "Teacher", null, null, null, null, null, null, minSalary, maxSalary, null, null),
                null,
                List.of(),
                List.of()
        );
    }

    private void stubLookup(List<Job> jobs, List<JobSkill> jobSkills, List<WorkTypeJob> workTypes) {
        List<String> jobIds = jobs.stream().map(Job::getId).toList();
        when(jobSkillRepository.findByJob_IdIn(jobIds)).thenReturn(jobSkills);
        when(workTypeJobRepository.findByJob_IdIn(jobIds)).thenReturn(workTypes);
    }

    private Job job(String id, String title) {
        return job(id, title, null);
    }

    private Job job(String id, String title, Location location) {
        return new Job(
                id,
                "Smart Company",
                title,
                "Description",
                null,
                null,
                null,
                "https://example.com/" + id,
                LocalDate.of(2026, 5, 20),
                null,
                null,
                null,
                null,
                null,
                location,
                null
        );
    }

    private Job jobWithRequiredExperience(String id, Integer requiredExperience) {
        return new Job(
                id,
                "Smart Company",
                "Teacher",
                "Description",
                requiredExperience,
                null,
                null,
                "https://example.com/" + id,
                LocalDate.of(2026, 5, 20),
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private Job jobWithEducation(String id, EducationLevel educationLevel) {
        return new Job(
                id,
                "Smart Company",
                "Teacher",
                "Description",
                null,
                null,
                null,
                "https://example.com/" + id,
                LocalDate.of(2026, 5, 20),
                null,
                null,
                null,
                null,
                null,
                null,
                educationLevel
        );
    }

    private Job jobWithSalary(String id, BigDecimal minSalary, BigDecimal maxSalary) {
        return new Job(
                id,
                "Smart Company",
                "Teacher",
                "Description",
                null,
                null,
                null,
                "https://example.com/" + id,
                LocalDate.of(2026, 5, 20),
                null,
                null,
                minSalary,
                maxSalary,
                null,
                null,
                null
        );
    }

    private Location location(String id, String city, String region, String country) {
        return new Location(id, null, city, region, country, null, null);
    }
}
