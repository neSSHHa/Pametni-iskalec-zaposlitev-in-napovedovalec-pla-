package si.um.feri.smartjobs.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import si.um.feri.smartjobs.ai.client.AiServiceClient;
import si.um.feri.smartjobs.ai.dto.AiJobFilterExtractionResponse;
import si.um.feri.smartjobs.ai.service.AiAllowedValuesService;
import si.um.feri.smartjobs.ai.service.AiJobFilterService;
import si.um.feri.smartjobs.ai.service.FastPromptFilterService;
import si.um.feri.smartjobs.educationLevel.entity.EducationLevel;
import si.um.feri.smartjobs.educationLevel.repository.EducationLevelRepository;
import si.um.feri.smartjobs.experienceLevel.entity.ExperienceLevel;
import si.um.feri.smartjobs.experienceLevel.repository.ExperienceLevelRepository;
import si.um.feri.smartjobs.job.dto.JobFilterRequest;
import si.um.feri.smartjobs.location.entity.Location;
import si.um.feri.smartjobs.location.repository.LocationRepository;
import si.um.feri.smartjobs.skill.entity.Skill;
import si.um.feri.smartjobs.skill.repository.SkillRepository;
import si.um.feri.smartjobs.workType.entity.WorkType;
import si.um.feri.smartjobs.workType.repository.WorkTypeRepository;

class AiFilterAllowedValuesTest {

    private SkillRepository skillRepository;
    private EducationLevelRepository educationLevelRepository;
    private ExperienceLevelRepository experienceLevelRepository;
    private WorkTypeRepository workTypeRepository;
    private LocationRepository locationRepository;
    private AiAllowedValuesService allowedValuesService;

    @BeforeEach
    void setUp() {
        skillRepository = mock(SkillRepository.class);
        educationLevelRepository = mock(EducationLevelRepository.class);
        experienceLevelRepository = mock(ExperienceLevelRepository.class);
        workTypeRepository = mock(WorkTypeRepository.class);
        locationRepository = mock(LocationRepository.class);

        when(skillRepository.findAll()).thenReturn(List.of(
                skill("Mechatronics"),
                skill("Hotel Service"),
                skill("Nursing")
        ));
        when(educationLevelRepository.findAll()).thenReturn(List.of(new EducationLevel("edu-1", "Bachelor")));
        when(experienceLevelRepository.findAll()).thenReturn(List.of(new ExperienceLevel("exp-1", "Junior")));
        when(workTypeRepository.findAll()).thenReturn(List.of(
                new WorkType("work-1", "Remote"),
                new WorkType("work-2", "Field work")
        ));
        when(locationRepository.findAll()).thenReturn(List.of(
                new Location("loc-1", null, "Vienna", "Vienna", "Austria", null, null),
                new Location("loc-2", null, "Graz", "Styria", "Austria", null, null)
        ));

        allowedValuesService = new AiAllowedValuesService(
                skillRepository,
                educationLevelRepository,
                experienceLevelRepository,
                workTypeRepository,
                locationRepository
        );
        allowedValuesService.refresh();
    }

    @Test
    void shouldRefreshAllowedValuesCache() {
        assertThat(allowedValuesService.getAllowedSkills()).contains("Mechatronics");

        when(skillRepository.findAll()).thenReturn(List.of(skill("Solar Installation")));

        allowedValuesService.refresh();

        assertThat(allowedValuesService.getAllowedSkills())
                .containsExactly("Solar Installation")
                .doesNotContain("Mechatronics");
    }

    @Test
    void shouldIncludeAustriaLocationsInAllowedValues() {
        assertThat(allowedValuesService.getAllowedLocations())
                .contains("Austria", "Vienna", "Graz", "Styria");
    }

    @Test
    void shouldIgnoreBlankAndDuplicateLocationsInAllowedValues() {
        when(locationRepository.findAll()).thenReturn(List.of(
                new Location("loc-1", null, "Vienna", "Vienna", "Austria", null, null),
                new Location("loc-2", "", "Vienna", null, "Austria", null, null)
        ));

        allowedValuesService.refresh();

        assertThat(allowedValuesService.getAllowedLocations())
                .containsExactly("Austria", "Vienna");
    }

    @Test
    void shouldHandleEmptyAllowedValuesRepositories() {
        when(skillRepository.findAll()).thenReturn(List.of());
        when(educationLevelRepository.findAll()).thenReturn(List.of());
        when(experienceLevelRepository.findAll()).thenReturn(List.of());
        when(workTypeRepository.findAll()).thenReturn(List.of());
        when(locationRepository.findAll()).thenReturn(List.of());

        allowedValuesService.refresh();

        assertThat(allowedValuesService.getAllowedSkills()).isEmpty();
        assertThat(allowedValuesService.getAllowedEducationLevels()).isEmpty();
        assertThat(allowedValuesService.getAllowedExperienceLevels()).isEmpty();
        assertThat(allowedValuesService.getAllowedWorkTypes()).isEmpty();
        assertThat(allowedValuesService.getAllowedLocations()).isEmpty();
    }

    @Test
    void shouldIncludeFieldWorkInAllowedValues() {
        assertThat(allowedValuesService.getAllowedWorkTypes()).contains("Field work");
    }

    @Test
    void shouldIncludeNewAustriaSkillsInAllowedValues() {
        assertThat(allowedValuesService.getAllowedSkills())
                .contains("Mechatronics", "Hotel Service", "Nursing");
    }

    @Test
    void shouldExtractCountryFromPrompt() {
        JobFilterRequest filter = fastPromptFilterService().buildFilter("Find me a job in Austria");

        assertThat(filter.location().country()).isEqualTo("Austria");
        assertThat(filter.location().countries()).containsExactly("Austria");
    }

    @Test
    void shouldExtractGermanCountryAliasesFromPrompt() {
        assertThat(fastPromptFilterService().buildFilter("Jobs in Osterreich").location().country())
                .isEqualTo("Austria");
        assertThat(fastPromptFilterService().buildFilter("Jobs in Slowenien").location().country())
                .isEqualTo("Slovenia");
        assertThat(fastPromptFilterService().buildFilter("Jobs in Deutschland").location().country())
                .isEqualTo("Germany");
    }

    @Test
    void shouldExtractWienAsViennaFromPrompt() {
        JobFilterRequest filter = fastPromptFilterService().buildFilter("Find jobs in Wien, Austria");

        assertThat(filter.location().city()).isEqualTo("Vienna");
        assertThat(filter.location().cities()).containsExactly("Vienna");
        assertThat(filter.location().country()).isEqualTo("Austria");
    }

    @Test
    void shouldExtractSkillFromPrompt() {
        JobFilterRequest filter = fastPromptFilterService().buildFilter("Mechatronics jobs in Graz");

        assertThat(filter.skills()).containsExactly("Mechatronics");
    }

    @Test
    void shouldExtractSkillFromSlovenianPromptAlias() {
        when(skillRepository.findAll()).thenReturn(List.of(skill("Teaching")));
        allowedValuesService.refresh();

        JobFilterRequest filter = fastPromptFilterService().buildFilter("Iscem delo kot uciteljica");

        assertThat(filter.skills()).containsExactly("Teaching");
    }

    @Test
    void shouldExtractSkillFromGermanPromptAlias() {
        JobFilterRequest filter = fastPromptFilterService().buildFilter("Ich suche Arbeit als Krankenschwester");

        assertThat(filter.skills()).containsExactly("Nursing");
    }

    @Test
    void shouldNotExtractSkillOutsideAllowedValues() {
        JobFilterRequest filter = fastPromptFilterService().buildFilter("Rust jobs in Graz");

        assertThat(filter.skills()).isEmpty();
    }

    @Test
    void shouldExtractWorkTypeFromPrompt() {
        JobFilterRequest filter = fastPromptFilterService().buildFilter("I want remote work in Vienna");

        assertThat(filter.workTypes()).containsExactly("Remote");
    }

    @Test
    void shouldNotExtractWorkTypeOutsideAllowedValues() {
        JobFilterRequest filter = fastPromptFilterService().buildFilter("I want shiftwork in Vienna");

        assertThat(filter.workTypes()).isEmpty();
    }

    @Test
    void shouldHandleEmptyPrompt() {
        JobFilterRequest filter = fastPromptFilterService().buildFilter("");

        assertThat(filter.skills()).isEmpty();
        assertThat(filter.workTypes()).isEmpty();
        assertThat(filter.location().country()).isNull();
        assertThat(filter.job().jobname()).isNull();
    }

    @Test
    void shouldHandleNullPrompt() {
        JobFilterRequest filter = fastPromptFilterService().buildFilter(null);

        assertThat(filter.skills()).isEmpty();
        assertThat(filter.workTypes()).isEmpty();
        assertThat(filter.location().country()).isNull();
        assertThat(filter.job().jobname()).isNull();
    }

    @Test
    void shouldFallbackWhenAiResponseInvalid() {
        AiServiceClient aiServiceClient = mock(AiServiceClient.class);
        FastPromptFilterService fastPromptFilterService = fastPromptFilterService();
        AiJobFilterService aiJobFilterService = new AiJobFilterService(
                aiServiceClient,
                allowedValuesService,
                fastPromptFilterService
        );

        when(aiServiceClient.extractJobFilter(eq("Mechatronics remote work in Austria"), anyList(), anyList(), anyList(), anyList()))
                .thenThrow(new IllegalStateException("Invalid AI response"));

        JobFilterRequest filter = aiJobFilterService.extractFilter("Mechatronics remote work in Austria");

        assertThat(filter.location().country()).isEqualTo("Austria");
        assertThat(filter.skills()).containsExactly("Mechatronics");
        assertThat(filter.workTypes()).containsExactly("Remote");
    }

    @Test
    void shouldFallbackWhenAiResponseEmpty() {
        AiServiceClient aiServiceClient = mock(AiServiceClient.class);
        AiJobFilterService aiJobFilterService = new AiJobFilterService(
                aiServiceClient,
                allowedValuesService,
                fastPromptFilterService()
        );

        when(aiServiceClient.extractJobFilter(eq("Mechatronics remote work in Austria"), anyList(), anyList(), anyList(), anyList()))
                .thenReturn(new AiJobFilterExtractionResponse(null, null, List.of(), List.of(), List.of()));

        JobFilterRequest filter = aiJobFilterService.extractFilter("Mechatronics remote work in Austria");

        assertThat(filter.location().country()).isEqualTo("Austria");
        assertThat(filter.skills()).containsExactly("Mechatronics");
        assertThat(filter.workTypes()).containsExactly("Remote");
    }

    @Test
    void shouldFallbackWhenAiResponseNull() {
        AiServiceClient aiServiceClient = mock(AiServiceClient.class);
        AiJobFilterService aiJobFilterService = new AiJobFilterService(
                aiServiceClient,
                allowedValuesService,
                fastPromptFilterService()
        );

        when(aiServiceClient.extractJobFilter(eq("Mechatronics remote work in Austria"), anyList(), anyList(), anyList(), anyList()))
                .thenReturn(null);

        JobFilterRequest filter = aiJobFilterService.extractFilter("Mechatronics remote work in Austria");

        assertThat(filter.location().country()).isEqualTo("Austria");
        assertThat(filter.skills()).containsExactly("Mechatronics");
        assertThat(filter.workTypes()).containsExactly("Remote");
    }

    @Test
    void shouldNotFallbackWhenAiResponseValid() {
        AiServiceClient aiServiceClient = mock(AiServiceClient.class);
        FastPromptFilterService fastPromptFilterService = mock(FastPromptFilterService.class);
        AiJobFilterService aiJobFilterService = new AiJobFilterService(
                aiServiceClient,
                allowedValuesService,
                fastPromptFilterService
        );
        AiJobFilterExtractionResponse aiResponse = new AiJobFilterExtractionResponse(
                null,
                new AiJobFilterExtractionResponse.LocationData(
                        null,
                        "Vienna",
                        List.of("Vienna"),
                        null,
                        List.of(),
                        "Austria",
                        List.of("Austria"),
                        null,
                        null
                ),
                List.of("Remote"),
                List.of("Mechatronics"),
                List.of()
        );

        when(aiServiceClient.extractJobFilter(eq("Mechatronics remote work in Austria"), anyList(), anyList(), anyList(), anyList()))
                .thenReturn(aiResponse);

        JobFilterRequest filter = aiJobFilterService.extractFilter("Mechatronics remote work in Austria");

        assertThat(filter.location().country()).isEqualTo("Austria");
        assertThat(filter.skills()).containsExactly("Mechatronics");
        assertThat(filter.workTypes()).containsExactly("Remote");
        verify(fastPromptFilterService, never()).buildFilter(anyString());
    }

    private FastPromptFilterService fastPromptFilterService() {
        return new FastPromptFilterService(allowedValuesService);
    }

    private Skill skill(String name) {
        return new Skill("skill-" + name.toLowerCase().replaceAll("[^a-z0-9]+", "-"), name, null);
    }
}
