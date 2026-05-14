package si.um.feri.smartjobs.seed.jobSkill;

import org.springframework.stereotype.Component;
import si.um.feri.smartjobs.job.repository.JobRepository;
import si.um.feri.smartjobs.jobSkill.entity.JobSkill;
import si.um.feri.smartjobs.jobSkill.repository.JobSkillRepository;
import si.um.feri.smartjobs.skill.repository.SkillRepository;

import java.util.ArrayList;
import java.util.List;

@Component
public class JobSkillSeed {

    private final JobSkillRepository jobSkillRepository;
    private final JobRepository jobRepository;
    private final SkillRepository skillRepository;

    public JobSkillSeed(JobSkillRepository jobSkillRepository,
                        JobRepository jobRepository,
                        SkillRepository skillRepository) {
        this.jobSkillRepository = jobSkillRepository;
        this.jobRepository = jobRepository;
        this.skillRepository = skillRepository;
    }

    public void seed() {
        if (jobSkillRepository.count() > 0) return;

        List<JobSkill> items = new ArrayList<>();

        add(items, "job-001", "skill-java", "skill-spring-boot", "skill-sql", "skill-git", "skill-docker");
        add(items, "job-002", "skill-java", "skill-spring-boot", "skill-hibernate", "skill-postgresql", "skill-git");
        add(items, "job-003", "skill-java", "skill-spring-boot", "skill-docker", "skill-kubernetes", "skill-ci-cd");
        add(items, "job-004", "skill-react", "skill-javascript", "skill-typescript", "skill-git");
        add(items, "job-005", "skill-react", "skill-javascript", "skill-communication", "skill-problem-solving");
        add(items, "job-006", "skill-docker", "skill-kubernetes", "skill-ci-cd", "skill-git");
        add(items, "job-007", "skill-sql", "skill-postgresql", "skill-mysql", "skill-problem-solving");
        add(items, "job-008", "skill-java", "skill-spring-boot", "skill-react", "skill-sql", "skill-git");
        add(items, "job-009", "skill-java", "skill-git", "skill-sql", "skill-problem-solving");
        add(items, "job-010", "skill-java", "skill-spring-boot", "skill-docker", "skill-communication", "skill-teamwork");
        add(items, "job-011", "skill-angular", "skill-typescript", "skill-git");
        add(items, "job-012", "skill-python", "skill-sql", "skill-postgresql", "skill-problem-solving");

        add(items, "job-013", "skill-nursing-care", "skill-patient-care", "skill-intensive-care", "skill-teamwork");
        add(items, "job-014", "skill-nursing-care", "skill-elderly-care", "skill-patient-care", "skill-reliability");
        add(items, "job-015", "skill-healthcare", "skill-ms-office", "skill-communication", "skill-slovenian");
        add(items, "job-016", "skill-nursing-care", "skill-intensive-care", "skill-patient-care", "skill-teamwork");
        add(items, "job-017", "skill-healthcare", "skill-communication", "skill-ms-office", "skill-driving-license-b");
        add(items, "job-018", "skill-healthcare", "skill-customer-service", "skill-communication", "skill-responsibility");

        add(items, "job-019", "skill-law", "skill-legal-research", "skill-bar-exam", "skill-ms-office");
        add(items, "job-020", "skill-law", "skill-court-procedures", "skill-legal-research");
        add(items, "job-021", "skill-administrative-procedures", "skill-public-procurement", "skill-communication");
        add(items, "job-022", "skill-public-procurement", "skill-administrative-procedures", "skill-ms-office");

        add(items, "job-023", "skill-accounting", "skill-bookkeeping", "skill-vat", "skill-tax-reporting");
        add(items, "job-024", "skill-bookkeeping", "skill-vasco", "skill-vat", "skill-payroll");
        add(items, "job-025", "skill-accounting", "skill-invoicing", "skill-ms-office", "skill-communication");
        add(items, "job-026", "skill-tax-reporting", "skill-vat", "skill-accounting");
        add(items, "job-027", "skill-payroll", "skill-accounting", "skill-ms-office");

        add(items, "job-028", "skill-graphic-design", "skill-adobe-creative-cloud", "skill-layout-design", "skill-photography");
        add(items, "job-029", "skill-graphic-design", "skill-adobe-creative-cloud", "skill-photo-editing", "skill-teamwork");
        add(items, "job-030", "skill-layout-design", "skill-graphic-design", "skill-adobe-creative-cloud");

        add(items, "job-031", "skill-printing", "skill-machine-operation", "skill-quality-control");
        add(items, "job-032", "skill-machine-operation", "skill-packaging", "skill-quality-control");
        add(items, "job-033", "skill-machine-operation", "skill-cnc", "skill-visual-inspection");
        add(items, "job-034", "skill-quality-control", "skill-visual-inspection", "skill-ms-office");
        add(items, "job-035", "skill-electrical-maintenance", "skill-technical-maintenance", "skill-maintenance");
        add(items, "job-036", "skill-mechanical-maintenance", "skill-maintenance", "skill-machine-operation");
        add(items, "job-037", "skill-assembly", "skill-packaging", "skill-visual-inspection", "skill-teamwork");

        add(items, "job-038", "skill-sales", "skill-customer-service", "skill-cash-register");
        add(items, "job-039", "skill-cash-register", "skill-customer-service", "skill-reliability");
        add(items, "job-040", "skill-sales", "skill-cash-register", "skill-teamwork");
        add(items, "job-041", "skill-sales", "skill-customer-service", "skill-communication", "skill-driving-license-b");
        add(items, "job-042", "skill-sales", "skill-customer-service", "skill-communication");

        add(items, "job-043", "skill-cooking", "skill-food-preparation", "skill-cleaning");
        add(items, "job-044", "skill-serving", "skill-customer-service", "skill-communication");
        add(items, "job-045", "skill-delivery", "skill-driving-license-b", "skill-customer-service");

        add(items, "job-046", "skill-warehouse-work", "skill-inventory-management", "skill-packaging");
        add(items, "job-047", "skill-truck-driving", "skill-delivery", "skill-driving-license-b", "skill-reliability");

        add(items, "job-048", "skill-teaching", "skill-classroom-management", "skill-communication");
        add(items, "job-049", "skill-teaching", "skill-classroom-management", "skill-communication", "skill-teamwork");

        add(items, "job-050", "skill-slovenian", "skill-english", "skill-communication", "skill-customer-service");

        jobSkillRepository.saveAll(items);
    }

private int counter = 1;

private void add(List<JobSkill> items, String jobId, String... skillIds) {
    for (String skillId : skillIds) {
        String id = "js-" + counter++;
        items.add(new JobSkill(
                id,
                jobRepository.findById(jobId).orElseThrow(),
                skillRepository.findById(skillId).orElseThrow()
        ));
    }
}
}