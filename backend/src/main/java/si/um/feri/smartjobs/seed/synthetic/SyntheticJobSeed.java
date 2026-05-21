package si.um.feri.smartjobs.seed.synthetic;

import org.springframework.stereotype.Component;
import si.um.feri.smartjobs.educationLevel.entity.EducationLevel;
import si.um.feri.smartjobs.educationLevel.repository.EducationLevelRepository;
import si.um.feri.smartjobs.experienceLevel.entity.ExperienceLevel;
import si.um.feri.smartjobs.experienceLevel.repository.ExperienceLevelRepository;
import si.um.feri.smartjobs.job.entity.Job;
import si.um.feri.smartjobs.job.repository.JobRepository;
import si.um.feri.smartjobs.jobSkill.entity.JobSkill;
import si.um.feri.smartjobs.jobSkill.repository.JobSkillRepository;
import si.um.feri.smartjobs.location.entity.Location;
import si.um.feri.smartjobs.location.repository.LocationRepository;
import si.um.feri.smartjobs.skill.entity.Skill;
import si.um.feri.smartjobs.skill.repository.SkillRepository;
import si.um.feri.smartjobs.skillType.entity.SkillType;
import si.um.feri.smartjobs.skillType.repository.SkillTypeRepository;
import si.um.feri.smartjobs.workType.entity.WorkType;
import si.um.feri.smartjobs.workType.repository.WorkTypeRepository;
import si.um.feri.smartjobs.workTypeJob.entity.WorkTypeJob;
import si.um.feri.smartjobs.workTypeJob.repository.WorkTypeJobRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
public class SyntheticJobSeed {

        private static final int TARGET_JOB_COUNT = 7_000;
        private static final int TARGET_SKILL_COUNT = 2_000;
        private static final int BATCH_SIZE = 500;

        private final JobRepository jobRepository;
        private final JobSkillRepository jobSkillRepository;
        private final WorkTypeJobRepository workTypeJobRepository;
        private final LocationRepository locationRepository;
        private final SkillRepository skillRepository;
        private final SkillTypeRepository skillTypeRepository;
        private final ExperienceLevelRepository experienceLevelRepository;
        private final EducationLevelRepository educationLevelRepository;
        private final WorkTypeRepository workTypeRepository;

        public SyntheticJobSeed(
                        JobRepository jobRepository,
                        JobSkillRepository jobSkillRepository,
                        WorkTypeJobRepository workTypeJobRepository,
                        LocationRepository locationRepository,
                        SkillRepository skillRepository,
                        SkillTypeRepository skillTypeRepository,
                        ExperienceLevelRepository experienceLevelRepository,
                        EducationLevelRepository educationLevelRepository,
                        WorkTypeRepository workTypeRepository) {
                this.jobRepository = jobRepository;
                this.jobSkillRepository = jobSkillRepository;
                this.workTypeJobRepository = workTypeJobRepository;
                this.locationRepository = locationRepository;
                this.skillRepository = skillRepository;
                this.skillTypeRepository = skillTypeRepository;
                this.experienceLevelRepository = experienceLevelRepository;
                this.educationLevelRepository = educationLevelRepository;
                this.workTypeRepository = workTypeRepository;
        }

        public void seedReferenceData() {
                ensureEuropeanLocations();
                ensureSkills();
        }

        public void seed() {
                seedReferenceData();

                Map<String, Skill> skills = skillRepository.findAll().stream()
                                .collect(
                                                LinkedHashMap::new,
                                                (map, skill) -> map.put(skill.getName().toLowerCase(), skill),
                                                LinkedHashMap::putAll);

                long existingJobs = jobRepository.count();

                if (existingJobs >= TARGET_JOB_COUNT) {
                        return;
                }

                List<Location> locations = locationRepository.findAll().stream()
                                .filter(location -> location.getCity() != null)
                                .sorted(
                                                Comparator.comparing(Location::getCountry,
                                                                Comparator.nullsLast(String::compareTo))
                                                                .thenComparing(Location::getCity,
                                                                                Comparator.nullsLast(
                                                                                                String::compareTo)))
                                .toList();

                List<ExperienceLevel> experienceLevels = experienceLevelRepository.findAll();
                List<EducationLevel> educationLevels = educationLevelRepository.findAll();
                Map<String, WorkType> workTypes = workTypes();

                Random random = new Random(20260521L);

                List<Job> jobBatch = new ArrayList<>();
                List<JobSkill> skillBatch = new ArrayList<>();
                List<WorkTypeJob> workTypeBatch = new ArrayList<>();

                int created = 0;

                for (int i = 1; jobRepository.count() + created < TARGET_JOB_COUNT; i++) {
                        String jobId = "syn-job-" + padded(i);

                        if (jobRepository.existsById(jobId)) {
                                continue;
                        }

                        RoleTemplate role = pick(ROLE_TEMPLATES, random);
                        Location location = weightedLocation(locations, random);
                        ExperienceLevel experience = weightedExperience(experienceLevels, random);
                        EducationLevel education = weightedEducation(educationLevels, random);

                        Salary salary = salaryFor(role, experience, location, random);
                        String company = companyName(role, random);
                        LocalDate posted = LocalDate.now().minusDays(random.nextInt(90));

                        Job job = new Job(
                                        jobId,
                                        company,
                                        titleFor(role, experience, random),
                                        descriptionFor(role, company, location, experience),
                                        requiredExperienceMonths(experience, random),
                                        BigDecimal.valueOf(salary.max() + random.nextInt(500)),
                                        BigDecimal.valueOf(Math.max(850, salary.min() - random.nextInt(250))),
                                        sourceFor(random),
                                        posted,
                                        posted,
                                        LocalDate.now(),
                                        BigDecimal.valueOf(salary.min()),
                                        BigDecimal.valueOf(salary.max()),
                                        experience,
                                        location,
                                        education);

                        jobBatch.add(job);
                        addJobSkills(skillBatch, job, role, skills, random);
                        addWorkTypes(workTypeBatch, job, workTypes, random, i);

                        created++;

                        if (jobBatch.size() >= BATCH_SIZE) {
                                flush(jobBatch, skillBatch, workTypeBatch);
                        }
                }

                flush(jobBatch, skillBatch, workTypeBatch);
        }

        private void flush(List<Job> jobs, List<JobSkill> skills, List<WorkTypeJob> workTypes) {
                if (jobs.isEmpty()) {
                        return;
                }

                jobRepository.saveAll(jobs);
                jobSkillRepository.saveAll(skills);
                workTypeJobRepository.saveAll(workTypes);

                jobs.clear();
                skills.clear();
                workTypes.clear();
        }

        private void ensureEuropeanLocations() {
                List<Location> missing = EUROPEAN_LOCATIONS.stream()
                                .filter(location -> !locationRepository.existsById(location.id()))
                                .map(location -> new Location(
                                                location.id(),
                                                null,
                                                location.city(),
                                                location.region(),
                                                location.country(),
                                                BigDecimal.valueOf(location.lat()),
                                                BigDecimal.valueOf(location.lng())))
                                .toList();

                if (!missing.isEmpty()) {
                        locationRepository.saveAll(missing);
                }
        }

        private Map<String, Skill> ensureSkills() {
                Map<String, Skill> byName = new LinkedHashMap<>();

                skillRepository.findAll().forEach(skill -> byName.put(skill.getName().toLowerCase(), skill));

                SkillType programming = skillTypeRepository.findById("st-programming-language").orElseThrow();
                SkillType framework = skillTypeRepository.findById("st-framework").orElseThrow();
                SkillType database = skillTypeRepository.findById("st-database").orElseThrow();
                SkillType cloud = skillTypeRepository.findById("st-cloud").orElseThrow();
                SkillType devops = skillTypeRepository.findById("st-devops").orElseThrow();
                SkillType itTool = skillTypeRepository.findById("st-it-tool").orElseThrow();
                SkillType softwareTool = skillTypeRepository.findById("st-software-tool").orElseThrow();
                SkillType soft = skillTypeRepository.findById("st-soft-skill").orElseThrow();
                SkillType design = skillTypeRepository.findById("st-design").orElseThrow();
                SkillType finance = skillTypeRepository.findById("st-finance").orElseThrow();
                SkillType healthcare = skillTypeRepository.findById("st-healthcare").orElseThrow();
                SkillType logistics = skillTypeRepository.findById("st-logistics").orElseThrow();
                SkillType manufacturing = skillTypeRepository.findById("st-manufacturing").orElseThrow();
                SkillType sales = skillTypeRepository.findById("st-sales").orElseThrow();

                List<Skill> missing = new ArrayList<>();

                addSkills(missing, byName, programming,
                                "Java", "C#", "Python", "JavaScript", "TypeScript", "Go", "Kotlin", "Swift",
                                "PHP", "Ruby", "Rust", "C++", "Scala", "Dart", "R", "Bash", "PowerShell", "SQL");

                addSkills(missing, byName, framework,
                                ".NET", "ASP.NET Core", "ASP.NET MVC", "Entity Framework", "Blazor",
                                "Spring Boot", "Hibernate", "React", "Angular", "Vue.js", "Next.js",
                                "Node.js", "Express.js", "NestJS", "Django", "FastAPI", "Flask",
                                "Laravel", "Symfony", "Ruby on Rails", "Flutter", "React Native",
                                "Redux", "Tailwind CSS", "RxJS", "NgRx");

                addSkills(missing, byName, database,
                                "PostgreSQL", "MySQL", "SQL Server", "Oracle", "MongoDB", "Redis",
                                "Elasticsearch", "DynamoDB", "Snowflake", "BigQuery", "MariaDB", "SQLite");

                addSkills(missing, byName, cloud,
                                "Azure", "AWS", "Google Cloud", "Azure Functions", "AWS Lambda",
                                "Azure DevOps", "Azure App Service", "Amazon ECS", "Amazon EKS",
                                "Google Kubernetes Engine", "Serverless", "Cloud Architecture", "Firebase");

                addSkills(missing, byName, devops,
                                "Docker", "Kubernetes", "CI/CD", "GitHub Actions", "GitLab CI", "Jenkins",
                                "Terraform", "Helm", "OpenShift", "Linux", "Ansible", "Argo CD",
                                "Prometheus", "Grafana", "Observability", "Monitoring", "Logging");

                addSkills(missing, byName, itTool,
                                "Git", "REST API", "GraphQL", "RabbitMQ", "Kafka", "gRPC", "OAuth2",
                                "Keycloak", "Postman", "Swagger", "OpenAPI", "Jira", "Confluence",
                                "HTML", "CSS", "Testing", "Android SDK", "iOS SDK", "Excel", "dbt", "Spark",
                                "SignalR", "CQRS", "MediatR", "LINQ", "Serilog");

                addSkills(missing, byName, softwareTool,
                                "Selenium", "Playwright", "Cypress", "Unit Testing", "Integration Testing",
                                "Clean Code", "Code Review", "Microservices", "Event Driven Architecture",
                                "Clean Architecture", "SOLID", "Power BI", "Tableau",
                                "Backend Developer", "Frontend Developer", "Full Stack Developer",
                                ".NET Backend Developer", "Java Backend Developer", "Java Angular Full Stack Developer",
                                "Full Stack .NET Java Developer");

                addSkills(missing, byName, design,
                                "Figma", "UX", "UI Design", "Design Systems", "Accessibility",
                                "Wireframing", "Prototyping", "User Research", "Product Design");

                addSkills(missing, byName, soft,
                                "Communication", "Teamwork", "Problem Solving", "Analytical Thinking",
                                "Mentoring", "Agile", "Scrum", "Kanban", "Leadership", "Ownership");

                addSkills(missing, byName, finance,
                                "Banking Systems", "Payment Systems", "Risk Management", "Financial Reporting",
                                "Accounting Systems", "ERP Finance", "Invoice Processing");

                addSkills(missing, byName, healthcare,
                                "Healthcare Systems", "Patient Data", "Medical Records", "Clinical Workflows",
                                "Healthcare Integration", "Data Privacy");

                addSkills(missing, byName, logistics,
                                "Logistics Systems", "Warehouse Systems", "Transport Management",
                                "Route Planning", "Inventory Management", "Supply Chain");

                addSkills(missing, byName, manufacturing,
                                "Manufacturing Systems", "Production Planning", "Quality Control",
                                "MES Systems", "Industrial Automation", "CNC");

                addSkills(missing, byName, sales,
                                "CRM Systems", "Salesforce", "Customer Service", "B2B Sales",
                                "Lead Management", "Customer Success");

                ensureSyntheticSkillVolume(missing, byName, itTool, framework, devops, database, cloud);

                if (!missing.isEmpty()) {
                        skillRepository.saveAll(missing);
                        missing.forEach(skill -> byName.put(skill.getName().toLowerCase(), skill));
                }

                return byName;
        }

        private void ensureSyntheticSkillVolume(
                        List<Skill> missing,
                        Map<String, Skill> byName,
                        SkillType itTool,
                        SkillType framework,
                        SkillType devops,
                        SkillType database,
                        SkillType cloud) {
                List<String> technologies = List.of(
                                "Java", "Spring Boot", "Hibernate", "C#", ".NET", "ASP.NET Core", "Entity Framework",
                                "React", "Angular", "Vue.js", "TypeScript", "JavaScript", "Python", "Django", "FastAPI",
                                "Node.js", "NestJS", "PostgreSQL", "MySQL", "SQL Server", "MongoDB", "Redis",
                                "Docker", "Kubernetes", "Azure", "AWS", "Google Cloud", "Terraform", "Kafka",
                                "RabbitMQ");

                List<String> contexts = List.of(
                                "API Development", "Microservices", "Cloud Integration", "Security",
                                "Performance Optimization",
                                "Testing", "Monitoring", "Enterprise Applications", "Banking Systems",
                                "Healthcare Systems",
                                "Logistics Systems", "Retail Systems", "Manufacturing Systems", "ERP Systems",
                                "CRM Systems",
                                "Data Processing", "Migration", "Architecture", "Automation", "Reporting",
                                "Integration",
                                "Workflow Automation", "Legacy Modernization", "Data Synchronization", "System Design",
                                "Backend Services", "Frontend Integration", "DevOps Pipelines", "Production Support");

                for (String technology : technologies) {
                        for (String context : contexts) {
                                if (byName.size() >= TARGET_SKILL_COUNT) {
                                        return;
                                }

                                String name = technology + " " + context;
                                SkillType type = detectSkillType(name, itTool, framework, devops, database, cloud);
                                addSkills(missing, byName, type, name);
                        }
                }

                int counter = 1;

                while (byName.size() < TARGET_SKILL_COUNT) {
                        String name = "Enterprise Digital Skill " + counter++;
                        addSkills(missing, byName, itTool, name);
                }
        }

        private SkillType detectSkillType(
                        String name,
                        SkillType itTool,
                        SkillType framework,
                        SkillType devops,
                        SkillType database,
                        SkillType cloud) {
                String lower = name.toLowerCase();

                if (lower.contains("azure") || lower.contains("aws") || lower.contains("google cloud")) {
                        return cloud;
                }

                if (lower.contains("docker") || lower.contains("kubernetes") || lower.contains("terraform")) {
                        return devops;
                }

                if (lower.contains("postgresql") || lower.contains("mysql") || lower.contains("sql server")
                                || lower.contains("mongodb") || lower.contains("redis")) {
                        return database;
                }

                if (lower.contains("spring") || lower.contains("hibernate") || lower.contains("asp.net")
                                || lower.contains(".net") || lower.contains("entity framework")
                                || lower.contains("react")
                                || lower.contains("angular") || lower.contains("vue") || lower.contains("django")
                                || lower.contains("fastapi") || lower.contains("nestjs")) {
                        return framework;
                }

                return itTool;
        }

        private void addSkills(List<Skill> missing, Map<String, Skill> byName, SkillType type, String... names) {
                for (String name : names) {
                        String key = name.toLowerCase();

                        if (!byName.containsKey(key)) {
                                Skill skill = new Skill(shortSkillId(name), name, type);
                                missing.add(skill);
                                byName.put(key, skill);
                        }
                }
        }

        private void addJobSkills(
                        List<JobSkill> items,
                        Job job,
                        RoleTemplate role,
                        Map<String, Skill> skills,
                        Random random) {
                List<String> picked = new ArrayList<>();

                picked.addAll(role.coreSkills());
                picked.addAll(randomSubset(role.secondarySkills(), 3 + random.nextInt(3), random));
                picked.addAll(randomSubset(role.niceToHaveSkills(), random.nextInt(3), random));

                picked.addAll(randomSubset(
                                List.of("Git", "Agile", "Scrum", "Communication", "Problem Solving", "Teamwork"),
                                1 + random.nextInt(2),
                                random));

                int index = 1;

                for (String skillName : picked.stream().distinct().limit(10).toList()) {
                        Skill skill = skills.get(skillName.toLowerCase());

                        if (skill != null) {
                                items.add(new JobSkill(
                                                "syn-js-" + job.getId().substring(8) + "-" + index++,
                                                job,
                                                skill));
                        }
                }
        }

        private void addWorkTypes(
                        List<WorkTypeJob> items,
                        Job job,
                        Map<String, WorkType> workTypes,
                        Random random,
                        int index) {
                String primary = random.nextInt(100) < 22
                                ? "wt-remote"
                                : random.nextInt(100) < 55
                                                ? "wt-hybrid"
                                                : "wt-onsite";

                WorkType primaryWorkType = workTypes.get(primary);
                WorkType fullTime = workTypes.get("wt-full-time");
                WorkType partTime = workTypes.get("wt-part-time");

                if (primaryWorkType != null) {
                        items.add(new WorkTypeJob("syn-wtj-" + padded(index) + "-1", job, primaryWorkType));
                }

                if (fullTime != null) {
                        items.add(new WorkTypeJob("syn-wtj-" + padded(index) + "-2", job, fullTime));
                }

                if (partTime != null && random.nextInt(100) < 7) {
                        items.add(new WorkTypeJob("syn-wtj-" + padded(index) + "-3", job, partTime));
                }
        }

        private Map<String, WorkType> workTypes() {
                Map<String, WorkType> result = new LinkedHashMap<>();
                workTypeRepository.findAll().forEach(workType -> result.put(workType.getId(), workType));
                return result;
        }

        private static Location weightedLocation(List<Location> locations, Random random) {
                int index = (int) Math.floor(Math.pow(random.nextDouble(), 1.35) * locations.size());
                return locations.get(Math.min(index, locations.size() - 1));
        }

        private static ExperienceLevel weightedExperience(List<ExperienceLevel> levels, Random random) {
                int roll = random.nextInt(100);

                String id = roll < 4
                                ? "exp-intern"
                                : roll < 12
                                                ? "exp-entry"
                                                : roll < 33
                                                                ? "exp-junior"
                                                                : roll < 68
                                                                                ? "exp-mid"
                                                                                : roll < 89
                                                                                                ? "exp-senior"
                                                                                                : roll < 97
                                                                                                                ? "exp-lead"
                                                                                                                : "exp-manager";

                return levels.stream()
                                .filter(level -> level.getId().equals(id))
                                .findFirst()
                                .orElse(levels.get(0));
        }

        private static EducationLevel weightedEducation(List<EducationLevel> levels, Random random) {
                int roll = random.nextInt(100);

                String id = roll < 7
                                ? "edu-secondary-vocational"
                                : roll < 16
                                                ? "edu-higher-vocational"
                                                : roll < 72
                                                                ? "edu-bachelor"
                                                                : roll < 93
                                                                                ? "edu-master"
                                                                                : "edu-not-specified";

                return levels.stream()
                                .filter(level -> level.getId().equals(id))
                                .findFirst()
                                .orElse(levels.get(0));
        }

        private static int requiredExperienceMonths(ExperienceLevel level, Random random) {
                return switch (level.getId()) {
                        case "exp-intern" -> random.nextInt(4);
                        case "exp-entry" -> 6 + random.nextInt(10);
                        case "exp-junior" -> 12 + random.nextInt(18);
                        case "exp-mid" -> 24 + random.nextInt(36);
                        case "exp-senior" -> 60 + random.nextInt(36);
                        case "exp-lead", "exp-manager" -> 84 + random.nextInt(48);
                        default -> 0;
                };
        }

        private static Salary salaryFor(RoleTemplate role, ExperienceLevel experience, Location location,
                        Random random) {
                double countryFactor = switch (location.getCountry()) {
                        case "Switzerland", "Norway" -> 1.90;
                        case "Germany", "Netherlands", "Sweden", "Denmark", "Ireland", "United Kingdom" -> 1.38;
                        case "Austria", "France", "Belgium", "Finland" -> 1.22;
                        case "Italy", "Spain", "Slovenia", "Czechia", "Estonia", "Portugal" -> 1.00;
                        case "Poland", "Croatia", "Hungary", "Slovakia", "Romania", "Bulgaria" -> 0.78;
                        default -> 0.90;
                };

                double levelFactor = switch (experience.getId()) {
                        case "exp-intern" -> 0.45;
                        case "exp-entry" -> 0.62;
                        case "exp-junior" -> 0.76;
                        case "exp-mid" -> 1.00;
                        case "exp-senior" -> 1.35;
                        case "exp-lead" -> 1.58;
                        case "exp-manager" -> 1.70;
                        default -> 0.90;
                };

                int min = (int) Math.round((role.baseSalary() * countryFactor * levelFactor) + random.nextInt(420));
                int max = min + 700 + random.nextInt(2600);

                return new Salary(roundTo50(min), roundTo50(max));
        }

        private static int roundTo50(int value) {
                return Math.max(850, Math.round(value / 50.0f) * 50);
        }

        private static String titleFor(RoleTemplate role, ExperienceLevel level, Random random) {
                String prefix = switch (level.getId()) {
                        case "exp-intern" -> "Intern ";
                        case "exp-entry" -> "Entry ";
                        case "exp-junior" -> "Junior ";
                        case "exp-senior" -> "Senior ";
                        case "exp-lead" -> random.nextBoolean() ? "Lead " : "Principal ";
                        case "exp-manager" -> "Engineering Manager ";
                        default -> "";
                };

                return prefix + pick(role.titles(), random);
        }

        private static String descriptionFor(RoleTemplate role, String company, Location location,
                        ExperienceLevel experience) {
                return company + " is hiring for a " + role.domain() + " position in "
                                + location.getCity() + ", " + location.getCountry() + ". "
                                + "The role focuses on " + String.join(", ", role.coreSkills()) + ". "
                                + "Additional useful skills include "
                                + String.join(", ", role.secondarySkills().stream().limit(5).toList()) + ". "
                                + "Seniority level: " + experience.getName() + ".";
        }

        private static String companyName(RoleTemplate role, Random random) {
                String domainWord = switch (role.domain()) {
                        case ".NET backend", "Java backend", "Python backend", "Full stack engineering" -> "Software";
                        case "React frontend", "Angular frontend", "UX/UI product design" -> "Digital";
                        case "DevOps platform", "Cloud engineering" -> "Cloud";
                        case "Data engineering", "Data analytics" -> "Data";
                        case "QA automation" -> "Quality";
                        case "Mobile development" -> "Mobile";
                        default -> "Tech";
                };

                return pick(COMPANY_PREFIXES, random) + " " + domainWord + " " + pick(COMPANY_SUFFIXES, random);
        }

        private static String sourceFor(Random random) {
                return pick(List.of(
                                "https://jobs.example.eu",
                                "https://career.example.eu",
                                "https://work.example.eu",
                                "https://talent.example.eu",
                                "https://itjobs.example.eu"), random);
        }

        private static <T> T pick(List<T> values, Random random) {
                return values.get(random.nextInt(values.size()));
        }

        private static List<String> randomSubset(List<String> values, int count, Random random) {
                List<String> copy = new ArrayList<>(values);
                List<String> result = new ArrayList<>();

                while (!copy.isEmpty() && result.size() < count) {
                        result.add(copy.remove(random.nextInt(copy.size())));
                }

                return result;
        }

        private static String padded(int value) {
                return String.format("%05d", value);
        }

        private static String slug(String value) {
                return value.toLowerCase()
                                .replace("#", "sharp")
                                .replace("+", "plus")
                                .replace(".", "")
                                .replace("/", "-")
                                .replaceAll("[^a-z0-9]+", "-")
                                .replaceAll("(^-|-$)", "");
        }

        private static String shortSkillId(String name) {
                String slug = slug(name);

                if (slug.length() > 12) {
                        slug = slug.substring(0, 12);
                }

                String hash = Integer.toHexString(name.toLowerCase().hashCode());

                if (hash.length() > 8) {
                        hash = hash.substring(0, 8);
                }

                return "sk-syn-" + slug + "-" + hash;
        }

        private record Salary(int min, int max) {
        }

        private record CityLocation(
                        String id,
                        String city,
                        String region,
                        String country,
                        double lat,
                        double lng) {
        }

        private record RoleTemplate(
                        String domain,
                        int baseSalary,
                        List<String> titles,
                        List<String> coreSkills,
                        List<String> secondarySkills,
                        List<String> niceToHaveSkills) {
        }

        private static final List<RoleTemplate> ROLE_TEMPLATES = List.of(
                        new RoleTemplate(
                                        ".NET backend",
                                        2850,
                                        List.of(
                                                        ".NET Developer",
                                                        "ASP.NET Core Backend Developer",
                                                        "C# Software Engineer",
                                                        ".NET API Developer",
                                                        "Azure .NET Developer"),
                                        List.of("C#", ".NET", "ASP.NET Core", "SQL Server"),
                                        List.of("Entity Framework", "Azure", "Docker", "REST API", "Microservices",
                                                        "Git", "CI/CD"),
                                        List.of("Kubernetes", "RabbitMQ", "Kafka", "OAuth2", "Blazor", "PostgreSQL")),
                        new RoleTemplate(
                                        "Java backend",
                                        2750,
                                        List.of(
                                                        "Java Developer",
                                                        "Spring Boot Developer",
                                                        "Backend Java Engineer",
                                                        "Java Microservices Developer"),
                                        List.of("Java", "Spring Boot", "SQL", "PostgreSQL"),
                                        List.of("Hibernate", "Docker", "Kubernetes", "REST API", "Git", "CI/CD",
                                                        "Microservices"),
                                        List.of("Kafka", "RabbitMQ", "AWS", "Redis", "Elasticsearch", "OAuth2")),
                        new RoleTemplate(
                                        "React frontend",
                                        2550,
                                        List.of(
                                                        "React Developer",
                                                        "Frontend Developer",
                                                        "TypeScript Frontend Engineer",
                                                        "UI Developer"),
                                        List.of("JavaScript", "TypeScript", "React", "HTML", "CSS"),
                                        List.of("Next.js", "REST API", "GraphQL", "Git", "Figma", "Accessibility"),
                                        List.of("Redux", "Tailwind CSS", "Cypress", "Playwright", "Design Systems")),
                        new RoleTemplate(
                                        "Angular frontend",
                                        2550,
                                        List.of(
                                                        "Angular Developer",
                                                        "Frontend Angular Engineer",
                                                        "TypeScript Developer"),
                                        List.of("TypeScript", "Angular", "HTML", "CSS"),
                                        List.of("RxJS", "REST API", "Git", "Figma", "Jira", "Accessibility"),
                                        List.of("NgRx", "Cypress", "Playwright", "Design Systems")),
                        new RoleTemplate(
                                        "DevOps platform",
                                        3300,
                                        List.of(
                                                        "DevOps Engineer",
                                                        "Platform Engineer",
                                                        "Site Reliability Engineer"),
                                        List.of("Docker", "Kubernetes", "Linux", "CI/CD"),
                                        List.of("Terraform", "Helm", "Prometheus", "Grafana", "GitHub Actions",
                                                        "GitLab CI"),
                                        List.of("AWS", "Azure", "Google Cloud", "Ansible", "Argo CD", "Observability")),
                        new RoleTemplate(
                                        "Cloud engineering",
                                        3400,
                                        List.of(
                                                        "Cloud Engineer",
                                                        "Azure Cloud Engineer",
                                                        "AWS Cloud Engineer",
                                                        "Cloud Infrastructure Engineer"),
                                        List.of("Azure", "AWS", "Docker", "Kubernetes"),
                                        List.of("Terraform", "Linux", "CI/CD", "Monitoring", "Cloud Architecture",
                                                        "Serverless"),
                                        List.of("Azure DevOps", "AWS Lambda", "Google Cloud", "Helm", "Prometheus")),
                        new RoleTemplate(
                                        "Data engineering",
                                        3200,
                                        List.of(
                                                        "Data Engineer",
                                                        "Analytics Engineer",
                                                        "ETL Developer",
                                                        "BI Data Engineer"),
                                        List.of("Python", "SQL", "PostgreSQL"),
                                        List.of("Snowflake", "BigQuery", "Kafka", "Docker", "Git", "Data Processing"),
                                        List.of("AWS", "Azure", "dbt", "Spark", "Power BI", "Tableau")),
                        new RoleTemplate(
                                        "Data analytics",
                                        2700,
                                        List.of(
                                                        "Data Analyst",
                                                        "BI Analyst",
                                                        "Reporting Analyst",
                                                        "Business Intelligence Developer"),
                                        List.of("SQL", "Power BI", "Data Processing"),
                                        List.of("Python", "Tableau", "BigQuery", "Snowflake", "Financial Reporting",
                                                        "Analytical Thinking"),
                                        List.of("PostgreSQL", "Excel", "ERP Systems", "CRM Systems")),
                        new RoleTemplate(
                                        "QA automation",
                                        2350,
                                        List.of(
                                                        "QA Automation Engineer",
                                                        "Test Automation Engineer",
                                                        "Software Tester"),
                                        List.of("Selenium", "Playwright", "Cypress", "Testing"),
                                        List.of("Java", "C#", "Python", "CI/CD", "Jira", "Git"),
                                        List.of("Postman", "REST API", "Docker", "SQL", "Integration Testing")),
                        new RoleTemplate(
                                        "Mobile development",
                                        2700,
                                        List.of(
                                                        "Mobile Developer",
                                                        "Android Developer",
                                                        "iOS Developer",
                                                        "Flutter Developer"),
                                        List.of("Kotlin", "Swift", "Flutter"),
                                        List.of("Dart", "React Native", "REST API", "Git", "Firebase"),
                                        List.of("Android SDK", "iOS SDK", "GraphQL", "SQLite")),
                        new RoleTemplate(
                                        "Python backend",
                                        2750,
                                        List.of(
                                                        "Python Developer",
                                                        "Backend Python Engineer",
                                                        "FastAPI Developer",
                                                        "Django Developer"),
                                        List.of("Python", "FastAPI", "Django", "PostgreSQL"),
                                        List.of("Docker", "REST API", "Redis", "Git", "CI/CD", "Microservices"),
                                        List.of("Kafka", "AWS", "Azure", "Celery", "MongoDB")),
                        new RoleTemplate(
                                        "Full stack engineering",
                                        2950,
                                        List.of(
                                                        "Full Stack Developer",
                                                        "Full Stack Engineer",
                                                        "Software Engineer"),
                                        List.of("JavaScript", "TypeScript", "React", "Node.js"),
                                        List.of("REST API", "PostgreSQL", "Docker", "Git", "CI/CD", "Microservices"),
                                        List.of("Next.js", "GraphQL", "AWS", "Azure", "MongoDB")),
                        new RoleTemplate(
                                        "UX/UI product design",
                                        2450,
                                        List.of(
                                                        "UX/UI Designer",
                                                        "Product Designer",
                                                        "UX Designer"),
                                        List.of("Figma", "UX", "UI Design", "Design Systems"),
                                        List.of("Accessibility", "Wireframing", "Prototyping", "User Research",
                                                        "Communication"),
                                        List.of("React", "HTML", "CSS", "Product Design")));

        private static final List<CityLocation> EUROPEAN_LOCATIONS = List.of(
                        new CityLocation("loc-vienna", "Vienna", "Vienna", "Austria", 48.2082, 16.3738),
                        new CityLocation("loc-graz", "Graz", "Styria", "Austria", 47.0707, 15.4395),
                        new CityLocation("loc-linz", "Linz", "Upper Austria", "Austria", 48.3069, 14.2858),
                        new CityLocation("loc-salzburg", "Salzburg", "Salzburg", "Austria", 47.8095, 13.0550),

                        new CityLocation("loc-munich", "Munich", "Bavaria", "Germany", 48.1351, 11.5820),
                        new CityLocation("loc-berlin", "Berlin", "Berlin", "Germany", 52.5200, 13.4050),
                        new CityLocation("loc-hamburg", "Hamburg", "Hamburg", "Germany", 53.5511, 9.9937),
                        new CityLocation("loc-frankfurt", "Frankfurt", "Hesse", "Germany", 50.1109, 8.6821),

                        new CityLocation("loc-zagreb", "Zagreb", "Zagreb", "Croatia", 45.8150, 15.9819),
                        new CityLocation("loc-split", "Split", "Dalmatia", "Croatia", 43.5081, 16.4402),

                        new CityLocation("loc-milan", "Milan", "Lombardy", "Italy", 45.4642, 9.1900),
                        new CityLocation("loc-rome", "Rome", "Lazio", "Italy", 41.9028, 12.4964),

                        new CityLocation("loc-amsterdam", "Amsterdam", "North Holland", "Netherlands", 52.3676, 4.9041),
                        new CityLocation("loc-rotterdam", "Rotterdam", "South Holland", "Netherlands", 51.9244, 4.4777),

                        new CityLocation("loc-zurich", "Zurich", "Zurich", "Switzerland", 47.3769, 8.5417),
                        new CityLocation("loc-geneva", "Geneva", "Geneva", "Switzerland", 46.2044, 6.1432),

                        new CityLocation("loc-paris", "Paris", "Ile-de-France", "France", 48.8566, 2.3522),
                        new CityLocation("loc-lyon", "Lyon", "Auvergne-Rhone-Alpes", "France", 45.7640, 4.8357),

                        new CityLocation("loc-madrid", "Madrid", "Madrid", "Spain", 40.4168, -3.7038),
                        new CityLocation("loc-barcelona", "Barcelona", "Catalonia", "Spain", 41.3874, 2.1686),

                        new CityLocation("loc-warsaw", "Warsaw", "Masovian", "Poland", 52.2297, 21.0122),
                        new CityLocation("loc-krakow", "Krakow", "Lesser Poland", "Poland", 50.0647, 19.9450),

                        new CityLocation("loc-prague", "Prague", "Prague", "Czechia", 50.0755, 14.4378),
                        new CityLocation("loc-brno", "Brno", "South Moravian", "Czechia", 49.1951, 16.6068),

                        new CityLocation("loc-bratislava", "Bratislava", "Bratislava", "Slovakia", 48.1486, 17.1077),
                        new CityLocation("loc-budapest", "Budapest", "Central Hungary", "Hungary", 47.4979, 19.0402),
                        new CityLocation("loc-bucharest", "Bucharest", "Bucharest", "Romania", 44.4268, 26.1025),
                        new CityLocation("loc-sofia-bg", "Sofia", "Sofia City", "Bulgaria", 42.6977, 23.3219),

                        new CityLocation("loc-stockholm", "Stockholm", "Stockholm", "Sweden", 59.3293, 18.0686),
                        new CityLocation("loc-copenhagen", "Copenhagen", "Capital Region", "Denmark", 55.6761, 12.5683),
                        new CityLocation("loc-oslo", "Oslo", "Oslo", "Norway", 59.9139, 10.7522),
                        new CityLocation("loc-helsinki", "Helsinki", "Uusimaa", "Finland", 60.1699, 24.9384),

                        new CityLocation("loc-dublin", "Dublin", "Leinster", "Ireland", 53.3498, -6.2603),
                        new CityLocation("loc-london", "London", "England", "United Kingdom", 51.5072, -0.1276),
                        new CityLocation("loc-lisbon", "Lisbon", "Lisbon", "Portugal", 38.7223, -9.1393),
                        new CityLocation("loc-tallinn", "Tallinn", "Harju", "Estonia", 59.4370, 24.7536));

        private static final List<String> COMPANY_PREFIXES = List.of(
                        "Nova", "Adria", "Alpine", "Blue", "Digital", "Smart", "Euro", "Cloud", "Data", "Fin",
                        "Inova", "Next", "Core", "Bright", "Vector", "Prime", "Urban", "Agile", "Code", "Future");

        private static final List<String> COMPANY_SUFFIXES = List.of(
                        "Labs", "Solutions", "Systems", "Group", "Works", "Technologies", "Partners", "Hub",
                        "Studio", "Factory", "Networks", "Services", "Engineering", "Consulting");
}
