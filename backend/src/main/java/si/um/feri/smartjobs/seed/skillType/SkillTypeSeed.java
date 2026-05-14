package si.um.feri.smartjobs.seed.skillType;

import org.springframework.stereotype.Component;
import si.um.feri.smartjobs.skillType.entity.SkillType;
import si.um.feri.smartjobs.skillType.repository.SkillTypeRepository;

import java.util.List;

@Component
public class SkillTypeSeed {

    private final SkillTypeRepository repository;

    public SkillTypeSeed(SkillTypeRepository repository) {
        this.repository = repository;
    }

    public void seed() {
        if (repository.count() > 0) return;

        repository.saveAll(List.of(
                new SkillType("st-programming-language", "Programming Language"),
                new SkillType("st-framework", "Framework"),
                new SkillType("st-database", "Database"),
                new SkillType("st-cloud", "Cloud"),
                new SkillType("st-devops", "DevOps"),
                new SkillType("st-it-tool", "IT Tool"),
                new SkillType("st-software-tool", "Software Tool"),
                new SkillType("st-soft-skill", "Soft Skill"),
                new SkillType("st-language", "Language"),
                new SkillType("st-education", "Education"),
                new SkillType("st-legal", "Legal"),
                new SkillType("st-finance", "Finance"),
                new SkillType("st-accounting", "Accounting"),
                new SkillType("st-medical", "Medical"),
                new SkillType("st-nursing", "Nursing"),
                new SkillType("st-healthcare", "Healthcare"),
                new SkillType("st-design", "Design"),
                new SkillType("st-sales", "Sales"),
                new SkillType("st-retail", "Retail"),
                new SkillType("st-production", "Production"),
                new SkillType("st-manufacturing", "Manufacturing"),
                new SkillType("st-mechanical", "Mechanical"),
                new SkillType("st-electrical", "Electrical"),
                new SkillType("st-logistics", "Logistics"),
                new SkillType("st-transportation", "Transportation"),
                new SkillType("st-hospitality", "Hospitality"),
                new SkillType("st-food-service", "Food Service"),
                new SkillType("st-administration", "Administration"),
                new SkillType("st-certification", "Certification"),
                new SkillType("st-other", "Other")
        ));
    }
}