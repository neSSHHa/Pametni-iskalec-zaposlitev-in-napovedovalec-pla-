// SkillSeed.java
package si.um.feri.smartjobs.seed.skill;

import org.springframework.stereotype.Component;
import si.um.feri.smartjobs.skill.entity.Skill;
import si.um.feri.smartjobs.skill.repository.SkillRepository;
import si.um.feri.smartjobs.skillType.repository.SkillTypeRepository;

import java.util.List;

@Component
public class SkillSeed {

    private final SkillRepository skillRepository;
    private final SkillTypeRepository skillTypeRepository;

    public SkillSeed(
            SkillRepository skillRepository,
            SkillTypeRepository skillTypeRepository
    ) {
        this.skillRepository = skillRepository;
        this.skillTypeRepository = skillTypeRepository;
    }

    public void seed() {

        if (skillRepository.count() > 0) return;

        var programming = skillTypeRepository.findById("st-programming-language").orElseThrow();
        var framework = skillTypeRepository.findById("st-framework").orElseThrow();
        var database = skillTypeRepository.findById("st-database").orElseThrow();
        var devops = skillTypeRepository.findById("st-devops").orElseThrow();
        var softwareTool = skillTypeRepository.findById("st-software-tool").orElseThrow();
        var softSkill = skillTypeRepository.findById("st-soft-skill").orElseThrow();
        var language = skillTypeRepository.findById("st-language").orElseThrow();
        var education = skillTypeRepository.findById("st-education").orElseThrow();
        var legal = skillTypeRepository.findById("st-legal").orElseThrow();
        var accounting = skillTypeRepository.findById("st-accounting").orElseThrow();
        var medical = skillTypeRepository.findById("st-medical").orElseThrow();
        var nursing = skillTypeRepository.findById("st-nursing").orElseThrow();
        var design = skillTypeRepository.findById("st-design").orElseThrow();
        var retail = skillTypeRepository.findById("st-retail").orElseThrow();
        var production = skillTypeRepository.findById("st-production").orElseThrow();
        var logistics = skillTypeRepository.findById("st-logistics").orElseThrow();
        var transportation = skillTypeRepository.findById("st-transportation").orElseThrow();
        var hospitality = skillTypeRepository.findById("st-hospitality").orElseThrow();
        var foodService = skillTypeRepository.findById("st-food-service").orElseThrow();
        var mechanical = skillTypeRepository.findById("st-mechanical").orElseThrow();
        var electrical = skillTypeRepository.findById("st-electrical").orElseThrow();
        var certification = skillTypeRepository.findById("st-certification").orElseThrow();

        skillRepository.saveAll(List.of(

                // PROGRAMMING
                new Skill("skill-java", "Java", programming),
                new Skill("skill-javascript", "JavaScript", programming),
                new Skill("skill-typescript", "TypeScript", programming),
                new Skill("skill-python", "Python", programming),
                new Skill("skill-csharp", "C#", programming),

                // FRAMEWORKS
                new Skill("skill-spring-boot", "Spring Boot", framework),
                new Skill("skill-hibernate", "Hibernate", framework),
                new Skill("skill-react", "React", framework),
                new Skill("skill-angular", "Angular", framework),
                new Skill("skill-vue", "Vue", framework),
                new Skill("skill-dotnet", ".NET", framework),
                new Skill("skill-asp-net-core", "ASP.NET Core", framework),
                new Skill("skill-entity-framework", "Entity Framework", framework),
                new Skill("skill-blazor", "Blazor", framework),

                // DATABASES
                new Skill("skill-sql", "SQL", database),
                new Skill("skill-postgresql", "PostgreSQL", database),
                new Skill("skill-mysql", "MySQL", database),
                new Skill("skill-mongodb", "MongoDB", database),

                // DEVOPS
                new Skill("skill-docker", "Docker", devops),
                new Skill("skill-kubernetes", "Kubernetes", devops),
                new Skill("skill-ci-cd", "CI/CD", devops),
                new Skill("skill-azure", "Azure", devops),

                // TOOLS
                new Skill("skill-git", "Git", softwareTool),
                new Skill("skill-jira", "Jira", softwareTool),
                new Skill("skill-ms-office", "MS Office", softwareTool),
                new Skill("skill-adobe-creative-cloud", "Adobe Creative Cloud", softwareTool),
                new Skill("skill-vasco", "Vasco", softwareTool),

                // SOFT SKILLS
                new Skill("skill-communication", "Communication", softSkill),
                new Skill("skill-teamwork", "Teamwork", softSkill),
                new Skill("skill-reliability", "Reliability", softSkill),
                new Skill("skill-responsibility", "Responsibility", softSkill),
                new Skill("skill-problem-solving", "Problem Solving", softSkill),

                // LANGUAGES
                new Skill("skill-english", "English", language),
                new Skill("skill-slovenian", "Slovenian", language),
                new Skill("skill-german", "German", language),
                new Skill("skill-croatian", "Croatian", language),

                // DESIGN
                new Skill("skill-graphic-design", "Graphic Design", design),
                new Skill("skill-layout-design", "Layout Design", design),
                new Skill("skill-photography", "Photography", design),
                new Skill("skill-photo-editing", "Photo Editing", design),

                // ACCOUNTING
                new Skill("skill-accounting", "Accounting", accounting),
                new Skill("skill-bookkeeping", "Bookkeeping", accounting),
                new Skill("skill-vat", "VAT", accounting),
                new Skill("skill-payroll", "Payroll", accounting),
                new Skill("skill-invoicing", "Invoicing", accounting),
                new Skill("skill-tax-reporting", "Tax Reporting", accounting),

                // LEGAL
                new Skill("skill-law", "Law", legal),
                new Skill("skill-legal-research", "Legal Research", legal),
                new Skill("skill-court-procedures", "Court Procedures", legal),
                new Skill("skill-public-procurement", "Public Procurement", legal),
                new Skill("skill-administrative-procedures", "Administrative Procedures", legal),

                // MEDICAL
                new Skill("skill-healthcare", "Healthcare", medical),
                new Skill("skill-patient-care", "Patient Care", medical),
                new Skill("skill-diagnostics", "Diagnostics", medical),

                // NURSING
                new Skill("skill-nursing-care", "Nursing Care", nursing),
                new Skill("skill-intensive-care", "Intensive Care", nursing),
                new Skill("skill-elderly-care", "Elderly Care", nursing),

                // EDUCATION
                new Skill("skill-teaching", "Teaching", education),
                new Skill("skill-classroom-management", "Classroom Management", education),

                // RETAIL
                new Skill("skill-sales", "Sales", retail),
                new Skill("skill-customer-service", "Customer Service", retail),
                new Skill("skill-cash-register", "Cash Register", retail),

                // PRODUCTION
                new Skill("skill-printing", "Printing", production),
                new Skill("skill-machine-operation", "Machine Operation", production),
                new Skill("skill-packaging", "Packaging", production),
                new Skill("skill-quality-control", "Quality Control", production),
                new Skill("skill-assembly", "Assembly", production),
                new Skill("skill-cnc", "CNC", production),
                new Skill("skill-visual-inspection", "Visual Inspection", production),
                new Skill("skill-maintenance", "Maintenance", production),
                new Skill("skill-technical-maintenance", "Technical Maintenance", production),

                // MECHANICAL
                new Skill("skill-mechanical-maintenance", "Mechanical Maintenance", mechanical),

                // ELECTRICAL
                new Skill("skill-electrical-maintenance", "Electrical Maintenance", electrical),

                // LOGISTICS
                new Skill("skill-warehouse-work", "Warehouse Work", logistics),
                new Skill("skill-inventory-management", "Inventory Management", logistics),

                // HOSPITALITY
                new Skill("skill-cleaning", "Cleaning", hospitality),
                new Skill("skill-laundry", "Laundry", hospitality),

                // FOOD SERVICE
                new Skill("skill-cooking", "Cooking", foodService),
                new Skill("skill-food-preparation", "Food Preparation", foodService),
                new Skill("skill-serving", "Serving", foodService),

                // TRANSPORTATION
                new Skill("skill-delivery", "Delivery", transportation),
                new Skill("skill-truck-driving", "Truck Driving", transportation),

                // CERTIFICATIONS
                new Skill("skill-driving-license-b", "Driving License B", certification),
                new Skill("skill-bar-exam", "Bar Exam", certification)
        ));
    }
}
