package si.um.feri.smartjobs.seed.userSkill;

import org.springframework.stereotype.Component;
import si.um.feri.smartjobs.skill.repository.SkillRepository;
import si.um.feri.smartjobs.user.repository.UserRepository;
import si.um.feri.smartjobs.userSkill.entity.UserSkill;
import si.um.feri.smartjobs.userSkill.repository.UserSkillRepository;

import java.util.ArrayList;
import java.util.List;

@Component
public class UserSkillSeed {

    private final UserSkillRepository userSkillRepository;
    private final UserRepository userRepository;
    private final SkillRepository skillRepository;

    public UserSkillSeed(UserSkillRepository userSkillRepository,
                         UserRepository userRepository,
                         SkillRepository skillRepository) {
        this.userSkillRepository = userSkillRepository;
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
    }

    public void seed() {
        if (userSkillRepository.count() > 0) return;

        List<UserSkill> items = new ArrayList<>();

        add(items, "user-001", "skill-java", "skill-spring-boot", "skill-sql", "skill-git");
        add(items, "user-002", "skill-java", "skill-spring-boot", "skill-postgresql", "skill-docker", "skill-hibernate");
        add(items, "user-003", "skill-nursing-care", "skill-patient-care", "skill-slovenian", "skill-teamwork");
        add(items, "user-004", "skill-accounting", "skill-bookkeeping", "skill-vat", "skill-payroll", "skill-tax-reporting");
        add(items, "user-005", "skill-graphic-design", "skill-adobe-creative-cloud", "skill-layout-design", "skill-photo-editing");
        add(items, "user-006", "skill-sales", "skill-customer-service", "skill-cash-register", "skill-communication");
        add(items, "user-007", "skill-machine-operation", "skill-maintenance", "skill-cnc", "skill-quality-control");
        add(items, "user-008", "skill-law", "skill-legal-research", "skill-court-procedures", "skill-bar-exam");
        add(items, "user-009", "skill-javascript", "skill-react", "skill-git", "skill-english");
        add(items, "user-010", "skill-teaching", "skill-classroom-management", "skill-communication", "skill-teamwork");
        add(items, "user-011", "skill-truck-driving", "skill-delivery", "skill-driving-license-b", "skill-reliability");
        add(items, "user-012", "skill-java", "skill-spring-boot", "skill-sql", "skill-english", "skill-communication");

        userSkillRepository.saveAll(items);
    }
private int counter = 1;

private void add(List<UserSkill> items, String userId, String... skillIds) {
    for (String skillId : skillIds) {
        String id = "us-" + counter++;
        items.add(new UserSkill(
                id,
                userRepository.findById(userId).orElseThrow(),
                skillRepository.findById(skillId).orElseThrow()
        ));
    }
}
}