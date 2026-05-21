package si.um.feri.smartjobs.job.service;

import org.springframework.stereotype.Service;
import si.um.feri.smartjobs.job.dto.JobDto;
import si.um.feri.smartjobs.job.dto.JobFilterRequest;
import si.um.feri.smartjobs.job.entity.Job;
import si.um.feri.smartjobs.job.repository.JobRepository;
import si.um.feri.smartjobs.jobSkill.repository.JobSkillRepository;
import si.um.feri.smartjobs.location.entity.Location;
import si.um.feri.smartjobs.skillRelation.entity.SkillRelation;
import si.um.feri.smartjobs.skillRelation.repository.SkillRelationRepository;
import si.um.feri.smartjobs.workTypeJob.repository.WorkTypeJobRepository;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class JobService {
    private static final double EXPERIENCE_TOLERANCE = 0.75;
    private static final double DIRECT_SKILL_MATCH = 1.0;
    private static final double TITLE_WEIGHT = 3.0;
    private static final double SKILL_WEIGHT = 4.0;
    private static final double LOCATION_WEIGHT = 1.4;
    private static final double WORK_TYPE_WEIGHT = 1.5;
    private static final double EXPERIENCE_WEIGHT = 1.5;
    private static final double EDUCATION_WEIGHT = 1.2;
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "or", "the", "for", "with", "of", "to", "in", "on", "at", "as",
            "i", "am", "me", "my", "job", "jobs", "role", "roles", "position", "work",
            "posao", "rad", "radim", "trazim", "zelim", "hoce", "hocu", "za", "sa", "u",
            "delovno", "mesto", "delo", "iscem", "sem"
    );
    private static final Map<String, List<String>> TERM_ALIASES = Map.ofEntries(
            Map.entry("frontend", List.of("front end", "front-end", "ui developer", "react", "angular", "vue", "javascript", "typescript")),
            Map.entry("front end", List.of("frontend", "front-end", "ui developer", "react", "angular", "vue", "javascript", "typescript")),
            Map.entry("backend", List.of("back end", "back-end", "server side", "api developer", "java", "spring boot", ".net", "c#", "python", "django", "fastapi")),
            Map.entry("back end", List.of("backend", "back-end", "server side", "api developer", "java", "spring boot", ".net", "c#", "python", "django", "fastapi")),
            Map.entry("full stack", List.of("fullstack", "full-stack", "software engineer", "frontend", "backend", "react", "node.js")),
            Map.entry("fullstack", List.of("full stack", "full-stack", "software engineer", "frontend", "backend", "react", "node.js")),
            Map.entry("developer", List.of("engineer", "software engineer", "software developer", "programmer", "programer", "razvijalec")),
            Map.entry("engineer", List.of("developer", "software engineer", "software developer", "inzenjer", "inzenir")),
            Map.entry("programer", List.of("developer", "software developer", "software engineer", "razvijalec")),
            Map.entry("razvijalec", List.of("developer", "software developer", "software engineer", "programer")),
            Map.entry("devops", List.of("platform engineer", "site reliability engineer", "sre", "kubernetes", "docker", "terraform")),
            Map.entry("data analyst", List.of("bi analyst", "reporting analyst", "business intelligence", "power bi", "tableau")),
            Map.entry("data engineer", List.of("analytics engineer", "etl developer", "bi data engineer", "python", "sql")),
            Map.entry("designer", List.of("ux designer", "ui designer", "product designer", "figma", "ux/ui")),
            Map.entry("remote", List.of("work from home", "wfh", "remote work", "na daljavo", "od kuce")),
            Map.entry("hybrid", List.of("hibrid", "hybrid work", "part remote")),
            Map.entry("on site", List.of("onsite", "on-site", "office", "pisarna"))
    );

    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final SkillRelationRepository skillRelationRepository;
    private final WorkTypeJobRepository workTypeJobRepository;

    public JobService(
            JobRepository jobRepository,
            JobSkillRepository jobSkillRepository,
            SkillRelationRepository skillRelationRepository,
            WorkTypeJobRepository workTypeJobRepository
    ) {
        this.jobRepository = jobRepository;
        this.jobSkillRepository = jobSkillRepository;
        this.skillRelationRepository = skillRelationRepository;
        this.workTypeJobRepository = workTypeJobRepository;
    }

    public List<JobDto> findAll() {
        return jobRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public List<JobDto> search(String query) {
        if (query == null || query.isBlank()) {
            return findAll();
        }

        return jobRepository
                .findByJobNameContainingIgnoreCaseOrCompanyNameContainingIgnoreCase(query, query)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public List<JobDto> filter(JobFilterRequest request) {
        if (request == null) {
            return findAll();
        }

        boolean hasCriteria = hasActiveCriteria(request);
        List<SkillRelation> skillRelations = isEmpty(request.skills()) ? List.of() : skillRelationRepository.findAll();

        return jobRepository.findAll().stream()
                .map(job -> new ScoredJob(job, calculateMatchScore(job, request, skillRelations)))
                .filter(scoredJob -> !hasCriteria || scoredJob.score().comparedFields() == 0
                        || scoredJob.score().matchedFields() > 0)
                .sorted(Comparator.comparingInt((ScoredJob scoredJob) -> scoredJob.score().percentage()).reversed())
                .map(scoredJob -> toDto(scoredJob.job(), scoredJob.score().percentage()))
                .toList();
    }

    private JobDto toDto(Job job) {
        return toDto(job, 100);
    }

    private JobDto toDto(Job job, int matchScore) {
        List<String> skills = jobSkillRepository.findByJob_Id(job.getId()).stream()
                .map(jobSkill -> jobSkill.getSkill().getName())
                .toList();

        String workMode = workTypeJobRepository.findByJob_Id(job.getId()).stream()
                .map(workTypeJob -> workTypeJob.getWorkType().getName())
                .reduce((first, second) -> first + ", " + second)
                .orElse("Unknown");

        Location location = job.getLocation();

        return new JobDto(
                job.getId(),
                job.getJobName(),
                job.getCompanyName(),
                job.getDescription(),
                formatLocation(location),
                location == null ? null : location.getCity(),
                location == null ? null : location.getRegion(),
                location == null ? null : location.getCountry(),
                location == null ? null : location.getLatitude(),
                location == null ? null : location.getLongitude(),
                workMode,
                job.getExperienceLevel() == null ? "Unknown" : job.getExperienceLevel().getName(),
                job.getEducationLevel() == null ? "Unknown" : job.getEducationLevel().getName(),
                job.getMinSalary(),
                job.getMaxSalary(),
                job.getDatePosted(),
                job.getSourceWebsite(),
                matchScore,
                toleranceLevel(matchScore),
                skills
        );
    }

    private MatchScore calculateMatchScore(Job job, JobFilterRequest request, List<SkillRelation> skillRelations) {
        MatchScore score = new MatchScore();
        JobFilterRequest.JobCriteria jobCriteria = request.job();
        JobFilterRequest.LocationCriteria locationCriteria = request.location();

        if (jobCriteria != null) {
            score.addText(job.getCompanyName(), jobCriteria.companyname());
            score.addText(job.getJobName(), jobCriteria.jobname(), TITLE_WEIGHT);
            score.addText(job.getDescription(), jobCriteria.description(), 0.7);
            score.addText(job.getSourceWebsite(), jobCriteria.sourceWebsite());
            score.addText(job.getExperienceLevel() == null ? null : job.getExperienceLevel().getName(),
                    jobCriteria.experienceLevelName(), EXPERIENCE_WEIGHT);
            score.addStrictTextOrId(
                    job.getEducationLevel() == null ? null : job.getEducationLevel().getId(),
                    job.getEducationLevel() == null ? null : job.getEducationLevel().getName(),
                    jobCriteria.educationLevel(),
                    EDUCATION_WEIGHT
            );
            score.addExact(job.getDatePosted(), jobCriteria.datePosted());
            score.addSalaryRange(job.getMinSalary(), job.getMaxSalary(), jobCriteria.minSalary(), jobCriteria.maxSalary());
            score.addSalaryRange(job.getPredictedMinSalary(), job.getPredictedMaxSalary(),
                    jobCriteria.predictedMinSalary(), jobCriteria.predictedMaxSalary());
            score.addExperience(job.getRequiredExperience(), jobCriteria.requiredExperience(), EXPERIENCE_WEIGHT);
        }

        if (locationCriteria != null && !isRemote(job)) {
            Location location = job.getLocation();
            score.addText(location == null ? null : location.getCityDistrict(), locationCriteria.cityDistrict(), LOCATION_WEIGHT);
            score.addText(location == null ? null : location.getCity(), locationCriteria.city(), LOCATION_WEIGHT);
            score.addText(location == null ? null : location.getRegion(), locationCriteria.region(), LOCATION_WEIGHT);
            score.addText(location == null ? null : location.getCountry(), locationCriteria.country(), LOCATION_WEIGHT);
            score.addExact(location == null ? null : location.getLatitude(), locationCriteria.latitude());
            score.addExact(location == null ? null : location.getLongitude(), locationCriteria.longitude());
        }

        addSkillScore(score, job, request.skills(), skillRelations);
        addWorkTypeScore(score, job, request.workTypes());

        return score;
    }

    private void addSkillScore(MatchScore score, Job job, List<String> requestedSkills, List<SkillRelation> skillRelations) {
        if (isEmpty(requestedSkills)) {
            return;
        }

        List<String> jobSkillValues = jobSkillRepository.findByJob_Id(job.getId()).stream()
                .flatMap(jobSkill -> List.of(jobSkill.getSkill().getId(), jobSkill.getSkill().getName()).stream())
                .map(this::normalize)
                .toList();

        List<Double> weights = requestedSkills.stream()
                .filter(this::hasText)
                .map(this::normalize)
                .map(requestedSkill -> skillMatchWeight(requestedSkill, jobSkillValues, skillRelations))
                .toList();

        if (weights.isEmpty()) {
            return;
        }

        double average = weights.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double best = weights.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        score.addWeighted(SKILL_WEIGHT, (average * 0.75) + (best * 0.25));
    }

    private double skillMatchWeight(String requestedSkill, List<String> jobSkillValues, List<SkillRelation> relations) {
        if (jobSkillValues.contains(requestedSkill)) {
            return DIRECT_SKILL_MATCH;
        }

        return relations.stream()
                .mapToDouble(relation -> relationMatchWeight(requestedSkill, jobSkillValues, relation))
                .max()
                .orElse(0.0);
    }

    private double relationMatchWeight(String requestedSkill, List<String> jobSkillValues, SkillRelation relation) {
        String sourceId = normalize(relation.getSourceSkill().getId());
        String sourceName = normalize(relation.getSourceSkill().getName());
        String targetId = normalize(relation.getTargetSkill().getId());
        String targetName = normalize(relation.getTargetSkill().getName());

        boolean requestedIsSource = requestedSkill.equals(sourceId) || requestedSkill.equals(sourceName);
        boolean requestedIsTarget = requestedSkill.equals(targetId) || requestedSkill.equals(targetName);
        boolean jobHasSource = jobSkillValues.contains(sourceId) || jobSkillValues.contains(sourceName);
        boolean jobHasTarget = jobSkillValues.contains(targetId) || jobSkillValues.contains(targetName);

        if ((requestedIsSource && jobHasTarget) || (requestedIsTarget && jobHasSource)) {
            return relationWeight(relation.getRelationshipType());
        }

        return 0.0;
    }

    private double relationWeight(String relationshipType) {
        return switch (relationshipType.toUpperCase(Locale.ROOT)) {
            case "FRAMEWORK_OF", "IMPLEMENTATION_OF" -> 0.8;
            case "SPECIALIZATION_OF" -> 0.7;
            case "PART_OF", "TOOL_FOR", "USED_WITH" -> 0.6;
            case "REQUIRES" -> 0.5;
            case "RELATED_TO", "SUPPORTS", "DATABASE_TECHNOLOGY" -> 0.4;
            default -> 0.4;
        };
    }

    private void addWorkTypeScore(MatchScore score, Job job, List<String> requestedWorkTypes) {
        if (isEmpty(requestedWorkTypes)) {
            return;
        }

        List<String> jobWorkTypes = jobWorkTypes(job);

        List<Double> weights = requestedWorkTypes.stream()
                .filter(this::hasText)
                .map(this::normalize)
                .map(requestedWorkType -> jobWorkTypes.stream()
                        .mapToDouble(jobWorkType -> textMatchWeight(jobWorkType, requestedWorkType))
                        .max()
                        .orElse(0.0))
                .toList();

        if (weights.isEmpty()) {
            return;
        }

        score.addWeighted(WORK_TYPE_WEIGHT, weights.stream().mapToDouble(Double::doubleValue).max().orElse(0.0));
    }

    private boolean hasActiveCriteria(JobFilterRequest request) {
        return hasActiveJobCriteria(request.job())
                || hasActiveLocationCriteria(request.location())
                || !isEmpty(request.skills())
                || !isEmpty(request.workTypes());
    }

    private boolean hasActiveJobCriteria(JobFilterRequest.JobCriteria criteria) {
        return criteria != null && (hasText(criteria.companyname())
                || hasText(criteria.jobname())
                || hasText(criteria.description())
                || hasText(criteria.sourceWebsite())
                || hasText(criteria.experienceLevelName())
                || hasText(criteria.educationLevel())
                || criteria.requiredExperience() != null
                || criteria.predictedMinSalary() != null
                || criteria.predictedMaxSalary() != null
                || criteria.datePosted() != null
                || criteria.minSalary() != null
                || criteria.maxSalary() != null);
    }

    private boolean hasActiveLocationCriteria(JobFilterRequest.LocationCriteria criteria) {
        return criteria != null && (hasText(criteria.cityDistrict())
                || hasText(criteria.city())
                || hasText(criteria.region())
                || hasText(criteria.country())
                || criteria.latitude() != null
                || criteria.longitude() != null);
    }

    private boolean isRemote(Job job) {
        return jobWorkTypes(job).stream().anyMatch(workType -> workType.contains("remote"));
    }

    private List<String> jobWorkTypes(Job job) {
        return workTypeJobRepository.findByJob_Id(job.getId()).stream()
                .flatMap(workTypeJob -> List.of(workTypeJob.getWorkType().getId(), workTypeJob.getWorkType().getName()).stream())
                .map(this::normalize)
                .toList();
    }

    private boolean isEmpty(List<String> values) {
        return values == null || values.stream().noneMatch(this::hasText);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        String withoutDiacritics = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return withoutDiacritics
                .toLowerCase(Locale.ROOT)
                .replace("č", "c")
                .replace("ć", "c")
                .replace("š", "s")
                .replace("ž", "z")
                .replace("đ", "dj")
                .replaceAll("[^a-z0-9+#.]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private double textMatchWeight(String actual, String expected) {
        if (!hasText(expected) || !hasText(actual)) {
            return 0.0;
        }

        String normalizedActual = normalize(actual);
        String normalizedExpected = normalize(expected);

        if (!hasText(normalizedActual) || !hasText(normalizedExpected)) {
            return 0.0;
        }

        if (normalizedActual.contains(normalizedExpected) || normalizedExpected.contains(normalizedActual)) {
            return 1.0;
        }

        Set<String> expectedTerms = expandedTerms(normalizedExpected);
        for (String term : expectedTerms) {
            if (term.length() > 2 && normalizedActual.contains(term)) {
                return 0.85;
            }
        }

        Set<String> actualTokens = significantTokens(normalizedActual);
        Set<String> expectedTokens = expectedTerms.stream()
                .flatMap(term -> significantTokens(term).stream())
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

        if (actualTokens.isEmpty() || expectedTokens.isEmpty()) {
            return 0.0;
        }

        long overlap = expectedTokens.stream().filter(actualTokens::contains).count();
        double ratio = (double) overlap / expectedTokens.size();

        if (ratio >= 0.66) {
            return 0.8;
        }
        if (ratio >= 0.34) {
            return 0.55;
        }

        return 0.0;
    }

    private Set<String> expandedTerms(String value) {
        Set<String> terms = new LinkedHashSet<>();
        terms.add(value);

        TERM_ALIASES.forEach((key, aliases) -> {
            if (value.contains(key) || significantTokens(value).contains(key)) {
                aliases.stream().map(this::normalize).forEach(terms::add);
            }
        });

        return terms;
    }

    private Set<String> significantTokens(String value) {
        return Arrays.stream(normalize(value).split(" "))
                .filter(this::hasText)
                .filter(token -> token.length() > 1)
                .filter(token -> !STOP_WORDS.contains(token))
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
    }

    private String formatLocation(Location location) {
        if (location == null) {
            return "Unknown";
        }

        return Arrays.asList(location.getCityDistrict(), location.getCity(), location.getRegion(), location.getCountry()).stream()
                .filter(this::hasText)
                .reduce((first, second) -> first + ", " + second)
                .orElse("Unknown");
    }

    private String toleranceLevel(int percentage) {
        if (percentage >= 80) {
            return "high";
        }
        if (percentage >= 50) {
            return "medium";
        }
        return "low";
    }

    private record ScoredJob(Job job, MatchScore score) {
    }

    private class MatchScore {
        private double matchedFields;
        private double comparedFields;

        void add(boolean matched) {
            add(matched ? 1.0 : 0.0);
        }

        void add(double matchWeight) {
            addWeighted(1.0, matchWeight);
        }

        void addWeighted(double fieldWeight, double matchWeight) {
            comparedFields += fieldWeight;
            matchedFields += fieldWeight * Math.max(0.0, Math.min(1.0, matchWeight));
        }

        void addText(String actual, String expected) {
            addText(actual, expected, 1.0);
        }

        void addText(String actual, String expected, double fieldWeight) {
            if (!hasText(expected) || !hasText(actual)) {
                return;
            }
            addWeighted(fieldWeight, textMatchWeight(actual, expected));
        }

        void addStrictTextOrId(String actualId, String actualName, String expected) {
            addStrictTextOrId(actualId, actualName, expected, 1.0);
        }

        void addStrictTextOrId(String actualId, String actualName, String expected, double fieldWeight) {
            if (!hasText(expected) || (!hasText(actualId) && !hasText(actualName))) {
                return;
            }

            String normalizedExpected = normalize(expected);
            boolean idMatches = hasText(actualId) && normalize(actualId).equals(normalizedExpected);
            double nameWeight = hasText(actualName) ? textMatchWeight(actualName, expected) : 0.0;
            addWeighted(fieldWeight, idMatches ? 1.0 : nameWeight);
        }

        void addExact(Object actual, Object expected) {
            if (expected == null || actual == null) {
                return;
            }
            add(actual.equals(expected));
        }

        void addSalaryRange(BigDecimal actualMin, BigDecimal actualMax, BigDecimal requestedMin, BigDecimal requestedMax) {
            if (requestedMin == null && requestedMax == null) {
                return;
            }
            if (actualMin == null && actualMax == null) {
                return;
            }

            BigDecimal min = actualMin == null ? actualMax : actualMin;
            BigDecimal max = actualMax == null ? actualMin : actualMax;
            boolean minMatches = requestedMin == null || max.compareTo(requestedMin) >= 0;
            boolean maxMatches = requestedMax == null || min.compareTo(requestedMax) <= 0;
            add(minMatches && maxMatches);
        }

        void addExperience(Integer requiredExperience, Integer userExperience, double fieldWeight) {
            if (requiredExperience == null || userExperience == null) {
                return;
            }

            int requiredMonths = toMonths(requiredExperience);
            int userMonths = toMonths(userExperience);
            if (userMonths >= requiredMonths) {
                addWeighted(fieldWeight, 1.0);
                return;
            }

            if (userMonths >= Math.ceil(requiredMonths * EXPERIENCE_TOLERANCE)) {
                addWeighted(fieldWeight, 0.8);
                return;
            }

            addWeighted(fieldWeight, Math.max(0.0, (double) userMonths / Math.max(requiredMonths, 1)) * 0.55);
        }

        double matchedFields() {
            return matchedFields;
        }

        double comparedFields() {
            return comparedFields;
        }

        int percentage() {
            if (comparedFields == 0) {
                return 100;
            }
            return (int) Math.round((matchedFields * 100.0) / comparedFields);
        }

        private int toMonths(Integer value) {
            return value <= 10 ? value * 12 : value;
        }
    }
}
