package si.um.feri.smartjobs.seed.skillRelation;

import org.springframework.stereotype.Component;
import si.um.feri.smartjobs.skill.repository.SkillRepository;
import si.um.feri.smartjobs.skillRelation.entity.SkillRelation;
import si.um.feri.smartjobs.skillRelation.repository.SkillRelationRepository;

import java.util.List;

@Component
public class SkillRelationSeed {

    private final SkillRelationRepository relationRepository;
    private final SkillRepository skillRepository;

    public SkillRelationSeed(SkillRelationRepository relationRepository, SkillRepository skillRepository) {
        this.relationRepository = relationRepository;
        this.skillRepository = skillRepository;
    }

    public void seed() {
        if (relationRepository.count() > 0) return;

        relationRepository.saveAll(List.of(
                rel("rel-spring-java", "FRAMEWORK_OF", "skill-spring-boot", "skill-java"),
                rel("rel-hibernate-java", "FRAMEWORK_OF", "skill-hibernate", "skill-java"),

                rel("rel-react-js", "FRAMEWORK_OF", "skill-react", "skill-javascript"),
                rel("rel-angular-ts", "FRAMEWORK_OF", "skill-angular", "skill-typescript"),
                rel("rel-vue-js", "FRAMEWORK_OF", "skill-vue", "skill-javascript"),

                rel("rel-postgres-sql", "IMPLEMENTATION_OF", "skill-postgresql", "skill-sql"),
                rel("rel-mysql-sql", "IMPLEMENTATION_OF", "skill-mysql", "skill-sql"),
                rel("rel-mongo-db", "DATABASE_TECHNOLOGY", "skill-mongodb", "skill-sql"),

                rel("rel-docker-devops", "PART_OF", "skill-docker", "skill-ci-cd"),
                rel("rel-kubernetes-devops", "PART_OF", "skill-kubernetes", "skill-docker"),

                rel("rel-bookkeeping-accounting", "PART_OF", "skill-bookkeeping", "skill-accounting"),
                rel("rel-vat-accounting", "PART_OF", "skill-vat", "skill-accounting"),
                rel("rel-payroll-accounting", "PART_OF", "skill-payroll", "skill-accounting"),
                rel("rel-invoicing-accounting", "PART_OF", "skill-invoicing", "skill-accounting"),
                rel("rel-tax-accounting", "PART_OF", "skill-tax-reporting", "skill-accounting"),
                rel("rel-vasco-accounting", "TOOL_FOR", "skill-vasco", "skill-accounting"),

                rel("rel-legalresearch-law", "PART_OF", "skill-legal-research", "skill-law"),
                rel("rel-court-law", "PART_OF", "skill-court-procedures", "skill-law"),
                rel("rel-procurement-law", "PART_OF", "skill-public-procurement", "skill-law"),
                rel("rel-admin-law", "PART_OF", "skill-administrative-procedures", "skill-law"),
                rel("rel-barexam-law", "CERTIFICATION_FOR", "skill-bar-exam", "skill-law"),

                rel("rel-patient-healthcare", "PART_OF", "skill-patient-care", "skill-healthcare"),
                rel("rel-diagnostics-healthcare", "PART_OF", "skill-diagnostics", "skill-healthcare"),
                rel("rel-nursing-healthcare", "PART_OF", "skill-nursing-care", "skill-healthcare"),
                rel("rel-intensive-nursing", "SPECIALIZATION_OF", "skill-intensive-care", "skill-nursing-care"),
                rel("rel-elderly-nursing", "SPECIALIZATION_OF", "skill-elderly-care", "skill-nursing-care"),

                rel("rel-adobe-design", "TOOL_FOR", "skill-adobe-creative-cloud", "skill-graphic-design"),
                rel("rel-layout-design", "PART_OF", "skill-layout-design", "skill-graphic-design"),
                rel("rel-photo-design", "PART_OF", "skill-photography", "skill-graphic-design"),
                rel("rel-photoediting-design", "PART_OF", "skill-photo-editing", "skill-graphic-design"),

                rel("rel-teaching-classroom", "RELATED_TO", "skill-classroom-management", "skill-teaching"),

                rel("rel-customer-sales", "PART_OF", "skill-customer-service", "skill-sales"),
                rel("rel-cash-sales", "PART_OF", "skill-cash-register", "skill-sales"),
                rel("rel-communication-sales", "SUPPORTS", "skill-communication", "skill-sales"),

                rel("rel-printing-machine", "USED_WITH", "skill-printing", "skill-machine-operation"),
                rel("rel-quality-production", "PART_OF", "skill-quality-control", "skill-machine-operation"),
                rel("rel-visual-quality", "PART_OF", "skill-visual-inspection", "skill-quality-control"),
                rel("rel-assembly-production", "PART_OF", "skill-assembly", "skill-machine-operation"),
                rel("rel-cnc-machine", "SPECIALIZATION_OF", "skill-cnc", "skill-machine-operation"),
                rel("rel-maintenance-machine", "SUPPORTS", "skill-maintenance", "skill-machine-operation"),
                rel("rel-technical-maintenance", "SPECIALIZATION_OF", "skill-technical-maintenance", "skill-maintenance"),
                rel("rel-mechanical-maintenance", "SPECIALIZATION_OF", "skill-mechanical-maintenance", "skill-maintenance"),
                rel("rel-electrical-maintenance", "SPECIALIZATION_OF", "skill-electrical-maintenance", "skill-maintenance"),

                rel("rel-warehouse-inventory", "RELATED_TO", "skill-inventory-management", "skill-warehouse-work"),
                rel("rel-packaging-warehouse", "RELATED_TO", "skill-packaging", "skill-warehouse-work"),

                rel("rel-foodprep-cooking", "PART_OF", "skill-food-preparation", "skill-cooking"),
                rel("rel-serving-food", "RELATED_TO", "skill-serving", "skill-food-preparation"),

                rel("rel-delivery-driving", "REQUIRES", "skill-delivery", "skill-driving-license-b"),
                rel("rel-truck-driving-license", "REQUIRES", "skill-truck-driving", "skill-driving-license-b"),

                rel("rel-cleaning-hospitality", "PART_OF", "skill-cleaning", "skill-serving"),
                rel("rel-laundry-hospitality", "RELATED_TO", "skill-laundry", "skill-cleaning"),

                rel("rel-teamwork-communication", "RELATED_TO", "skill-teamwork", "skill-communication"),
                rel("rel-responsibility-reliability", "RELATED_TO", "skill-responsibility", "skill-reliability"),
                rel("rel-problem-solving-communication", "SUPPORTS", "skill-problem-solving", "skill-communication")
        ));
    }

    private SkillRelation rel(String id, String type, String sourceSkillId, String targetSkillId) {
        return new SkillRelation(
                id,
                type,
                skillRepository.findById(sourceSkillId).orElseThrow(),
                skillRepository.findById(targetSkillId).orElseThrow()
        );
    }
}