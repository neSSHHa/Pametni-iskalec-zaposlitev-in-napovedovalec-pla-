package si.um.feri.smartjobs.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class DatabaseImportIntegrityIntegrationTest extends AbstractIntegrationTestData {

    @Test
    void shouldImportAllReferenceTables() {
        assertThat(educationLevelRepository.count()).isGreaterThan(0);
        assertThat(experienceLevelRepository.count()).isGreaterThan(0);
        assertThat(locationRepository.count()).isGreaterThan(0);
        assertThat(skillTypeRepository.count()).isGreaterThan(0);
        assertThat(skillRepository.count()).isGreaterThan(0);
        assertThat(workTypeRepository.count()).isGreaterThan(0);
    }

    @Test
    void shouldImportSloveniaAndAustriaJobs() {
        assertThat(jobRepository.findByLocation_CountryContainingIgnoreCase("Austria")).hasSize(3);
        assertThat(jobRepository.findByLocation_CountryContainingIgnoreCase("Slovenia")).hasSize(1);
    }

    @Test
    void shouldImportAustriaLocations() {
        assertThat(locationRepository.findAll())
                .extracting(location -> location.getCountry())
                .contains("Austria");
    }

    @Test
    void shouldContainAustriaCountry() {
        assertThat(locationRepository.findAll().stream().anyMatch(location -> "Austria".equals(location.getCountry()))).isTrue();
    }

    @Test
    void shouldContainWienLocation() {
        assertThat(locationRepository.findAll())
                .extracting(location -> location.getCity())
                .contains("Vienna");
    }

    @Test
    void shouldImportSkills() {
        assertThat(skillRepository.findAll())
                .extracting(skill -> skill.getName())
                .contains("React", "Mechatronics", "Nursing");
    }

    @Test
    void shouldImportSkillRelations() {
        assertThat(skillRelationRepository.findAll()).hasSize(1);
    }

    @Test
    void shouldImportJobSkills() {
        assertThat(jobSkillRepository.count()).isEqualTo(5);
    }

    @Test
    void shouldImportWorkTypeJobs() {
        assertThat(workTypeJobRepository.count()).isEqualTo(5);
    }

    @Test
    void shouldContainExpectedWorkTypesFromSeed() {
        assertThat(workTypeRepository.findAll())
                .extracting(workType -> workType.getName())
                .containsExactlyInAnyOrder("Remote", "Hybrid", "On-site", "Field work", "Not specified");
    }

    @Test
    void shouldHaveNoBrokenJobSkillReferences() {
        assertThat(jobSkillRepository.findAll())
                .allSatisfy(jobSkill -> {
                    assertThat(jobSkill.getJob()).isNotNull();
                    assertThat(jobSkill.getSkill()).isNotNull();
                });
    }

    @Test
    void shouldHaveNoBrokenWorkTypeJobReferences() {
        assertThat(workTypeJobRepository.findAll())
                .allSatisfy(workTypeJob -> {
                    assertThat(workTypeJob.getJob()).isNotNull();
                    assertThat(workTypeJob.getWorkType()).isNotNull();
                });
    }

    @Test
    void shouldHaveNoBrokenSkillRelationReferences() {
        assertThat(skillRelationRepository.findAll())
                .allSatisfy(relation -> {
                    assertThat(relation.getSourceSkill()).isNotNull();
                    assertThat(relation.getTargetSkill()).isNotNull();
                });
    }

    @Test
    void shouldHaveNoJobsWithoutValidLocation() {
        assertThat(jobRepository.findAll()).allSatisfy(job -> assertThat(job.getLocation()).isNotNull());
    }

    @Test
    void shouldHaveNoJobsWithoutValidExperienceLevel() {
        assertThat(jobRepository.findAll()).allSatisfy(job -> assertThat(job.getExperienceLevel()).isNotNull());
    }

    @Test
    void shouldHaveNoJobsWithoutValidEducationLevel() {
        assertThat(jobRepository.findAll()).allSatisfy(job -> assertThat(job.getEducationLevel()).isNotNull());
    }

    @Test
    void shouldKeepSourceJobKeysUnique() {
        Set<String> sourceKeys = jobRepository.findAll().stream()
                .map(job -> job.getSourceJobKey())
                .collect(Collectors.toSet());

        assertThat(sourceKeys).hasSize((int) jobRepository.count());
    }
}
