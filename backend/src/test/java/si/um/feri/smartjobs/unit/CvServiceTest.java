package si.um.feri.smartjobs.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import si.um.feri.smartjobs.ai.dto.AiJobFilterDebugResponse;
import si.um.feri.smartjobs.ai.service.AiAllowedValuesService;
import si.um.feri.smartjobs.ai.service.AiJobFilterService;
import si.um.feri.smartjobs.cv.dto.CvJobMatchResponse;
import si.um.feri.smartjobs.cv.service.CvJobMatchingService;
import si.um.feri.smartjobs.cv.service.CvProfileFilterService;
import si.um.feri.smartjobs.cv.service.CvTextExtractionService;
import si.um.feri.smartjobs.job.dto.JobDto;
import si.um.feri.smartjobs.job.dto.JobFilterRequest;
import si.um.feri.smartjobs.job.dto.JobSearchResponse;
import si.um.feri.smartjobs.job.service.JobService;

class CvServiceTest {

    @Test
    void shouldExtractAndTrimTextFromCvFile() {
        CvTextExtractionService service = new CvTextExtractionService();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cv.txt",
                "text/plain",
                "  Java backend developer  ".getBytes()
        );

        String text = service.extractText(file);

        assertThat(text).isEqualTo("Java backend developer");
    }

    @Test
    void shouldRejectEmptyCvFile() {
        CvTextExtractionService service = new CvTextExtractionService();
        MockMultipartFile file = new MockMultipartFile("file", "cv.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> service.extractText(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CV file is required.");
    }

    @Test
    void shouldRejectMissingCvFile() {
        CvTextExtractionService service = new CvTextExtractionService();

        assertThatThrownBy(() -> service.extractText(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CV file is required.");
    }

    @Test
    void shouldWrapCvReadFailure() throws IOException {
        CvTextExtractionService service = new CvTextExtractionService();
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getInputStream()).thenThrow(new IOException("Cannot read file"));

        assertThatThrownBy(() -> service.extractText(file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Could not extract text from CV file.")
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void shouldExtractAllowedTechnicalSkillsAndIgnoreSoftSkills() {
        CvProfileFilterService service = profileServiceWithAllowedSkills(
                "Java",
                "Spring Boot",
                "Docker",
                "communication"
        );

        JobFilterRequest filter = service.buildFilter(
                "Communication skills. Java backend developer using Spring Boot and Docker."
        );

        assertThat(filter.skills()).containsExactlyInAnyOrder("Java", "Spring Boot", "Docker");
        assertThat(filter.skills()).doesNotContain("communication");
    }

    @Test
    void shouldExtractSkillAliasOnlyWhenCanonicalSkillIsAllowed() {
        CvProfileFilterService service = profileServiceWithAllowedSkills(".NET");

        JobFilterRequest filter = service.buildFilter("Built backend services with dotnet.");

        assertThat(filter.skills()).containsExactly(".NET");
    }

    @Test
    void shouldIgnoreCvSkillOutsideAllowedValues() {
        CvProfileFilterService service = profileServiceWithAllowedSkills("Java");

        JobFilterRequest filter = service.buildFilter("Rust developer with Docker experience.");

        assertThat(filter.skills()).isEmpty();
    }

    @Test
    void shouldInferJavaBackendDeveloperRole() {
        CvProfileFilterService service = profileServiceWithAllowedSkills("Java", "Spring Boot");

        JobFilterRequest filter = service.buildFilter("Java and Spring Boot backend developer.");

        assertThat(filter.job().jobname()).isEqualTo("Java Backend Developer");
    }

    @Test
    void shouldInferJuniorExperienceLevelForStudent() {
        CvProfileFilterService service = profileServiceWithAllowedSkills("Java");

        JobFilterRequest filter = service.buildFilter("Computer science student with Java experience.");

        assertThat(filter.job().experienceLevelName()).isEqualTo("Junior");
    }

    @Test
    void shouldHandleNullCvText() {
        CvProfileFilterService service = profileServiceWithAllowedSkills("Java");

        JobFilterRequest filter = service.buildFilter(null);

        assertThat(filter.skills()).isEmpty();
        assertThat(filter.job().jobname()).isEqualTo("Software Developer");
        assertThat(filter.job().experienceLevelName()).isNull();
    }

    @Test
    void shouldUseLocalProfileFilterInFastMode() {
        CvTextExtractionService textExtractionService = mock(CvTextExtractionService.class);
        CvProfileFilterService profileFilterService = mock(CvProfileFilterService.class);
        AiJobFilterService aiJobFilterService = mock(AiJobFilterService.class);
        JobService jobService = mock(JobService.class);
        CvJobMatchingService service = new CvJobMatchingService(
                textExtractionService,
                profileFilterService,
                aiJobFilterService,
                jobService
        );
        MockMultipartFile file = cvFile();
        JobFilterRequest filter = emptyFilter();
        JobDto job = mock(JobDto.class);
        JobSearchResponse rankedJobs = new JobSearchResponse(List.of(job), 1, 0, 50, false, 72, filter);

        when(textExtractionService.extractText(file)).thenReturn("Java backend developer");
        when(profileFilterService.buildFilter("Java backend developer")).thenReturn(filter);
        when(jobService.filterResponse(filter)).thenReturn(rankedJobs);

        CvJobMatchResponse response = service.matchJobs(file, "fast");

        assertThat(response.filename()).isEqualTo("cv.txt");
        assertThat(response.contentType()).isEqualTo("text/plain");
        assertThat(response.extractedText()).isEqualTo("Java backend developer");
        assertThat(response.filterRequest()).isSameAs(filter);
        assertThat(response.jobs()).containsExactly(job);
        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.averageMatch()).isEqualTo(72);
        verify(aiJobFilterService, never()).extractCvFilter("Java backend developer");
    }

    @Test
    void shouldUseAiProfileFilterInThinkingMode() {
        CvTextExtractionService textExtractionService = mock(CvTextExtractionService.class);
        CvProfileFilterService profileFilterService = mock(CvProfileFilterService.class);
        AiJobFilterService aiJobFilterService = mock(AiJobFilterService.class);
        JobService jobService = mock(JobService.class);
        CvJobMatchingService service = new CvJobMatchingService(
                textExtractionService,
                profileFilterService,
                aiJobFilterService,
                jobService
        );
        MockMultipartFile file = cvFile();
        JobFilterRequest filter = emptyFilter();
        JobSearchResponse rankedJobs = new JobSearchResponse(List.of(), 0, 0, 50, false, null, filter);

        when(textExtractionService.extractText(file)).thenReturn("Java backend developer");
        when(aiJobFilterService.extractCvFilter("Java backend developer")).thenReturn(filter);
        when(jobService.filterResponse(filter)).thenReturn(rankedJobs);

        service.matchJobs(file, "THINKING");

        verify(aiJobFilterService).extractCvFilter("Java backend developer");
        verify(profileFilterService, never()).buildFilter("Java backend developer");
    }

    @Test
    void shouldDelegateCvDebugExtractionToAiService() {
        CvTextExtractionService textExtractionService = mock(CvTextExtractionService.class);
        AiJobFilterService aiJobFilterService = mock(AiJobFilterService.class);
        CvJobMatchingService service = new CvJobMatchingService(
                textExtractionService,
                mock(CvProfileFilterService.class),
                aiJobFilterService,
                mock(JobService.class)
        );
        MockMultipartFile file = cvFile();
        AiJobFilterDebugResponse debugResponse = mock(AiJobFilterDebugResponse.class);

        when(textExtractionService.extractText(file)).thenReturn("Java backend developer");
        when(aiJobFilterService.extractCvDebug("Java backend developer")).thenReturn(debugResponse);

        assertThat(service.extractFilterDebug(file)).isSameAs(debugResponse);
    }

    @Test
    void shouldDelegateCvProfileRewriteToAiService() {
        AiJobFilterService aiJobFilterService = mock(AiJobFilterService.class);
        CvJobMatchingService service = new CvJobMatchingService(
                mock(CvTextExtractionService.class),
                mock(CvProfileFilterService.class),
                aiJobFilterService,
                mock(JobService.class)
        );
        when(aiJobFilterService.rewriteCvToProfileText("Original CV")).thenReturn("Rewritten profile");

        assertThat(service.rewriteCvToProfileText("Original CV")).isEqualTo("Rewritten profile");
    }

    private CvProfileFilterService profileServiceWithAllowedSkills(String... skills) {
        AiAllowedValuesService allowedValuesService = mock(AiAllowedValuesService.class);
        when(allowedValuesService.getAllowedSkills()).thenReturn(List.of(skills));
        return new CvProfileFilterService(allowedValuesService);
    }

    private MockMultipartFile cvFile() {
        return new MockMultipartFile("file", "cv.txt", "text/plain", "CV contents".getBytes());
    }

    private JobFilterRequest emptyFilter() {
        return new JobFilterRequest(null, null, List.of(), List.of());
    }
}
