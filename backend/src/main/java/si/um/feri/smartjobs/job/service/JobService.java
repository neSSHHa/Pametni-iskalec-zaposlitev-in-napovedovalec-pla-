package si.um.feri.smartjobs.job.service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import si.um.feri.smartjobs.job.dto.JobDto;
import si.um.feri.smartjobs.job.dto.JobFilterRequest;
import si.um.feri.smartjobs.job.dto.JobSearchResponse;
import si.um.feri.smartjobs.job.entity.Job;
import si.um.feri.smartjobs.job.repository.JobRepository;
import si.um.feri.smartjobs.jobSkill.entity.JobSkill;
import si.um.feri.smartjobs.jobSkill.repository.JobSkillRepository;
import si.um.feri.smartjobs.location.entity.Location;
import si.um.feri.smartjobs.skillRelation.entity.SkillRelation;
import si.um.feri.smartjobs.skillRelation.repository.SkillRelationRepository;
import si.um.feri.smartjobs.workTypeJob.entity.WorkTypeJob;
import si.um.feri.smartjobs.workTypeJob.repository.WorkTypeJobRepository;

@Service
public class JobService {
    private static final double EXPERIENCE_TOLERANCE = 0.75;
    private static final double DIRECT_SKILL_MATCH = 1.0;

    // Skupaj 100 tock. Ce job nima podatka, se njegova teza ne steje v availablePoints.
    private static final double SKILL_WEIGHT = 40.0;
    private static final double TITLE_WEIGHT = 20.0;
    private static final double EXPERIENCE_WEIGHT = 15.0;
    private static final double LOCATION_WEIGHT = 10.0;
    private static final double WORK_TYPE_WEIGHT = 7.0;
    private static final double EDUCATION_WEIGHT = 5.0;
    private static final double SALARY_WEIGHT = 3.0;

    public static final int DEFAULT_PAGE_SIZE = 50;
    public static final int MAX_PAGE_SIZE = 200;
    public static final int DEFAULT_MATCH_LIMIT = 200;
    public static final int DEFAULT_MIN_MATCH_SCORE = 30;

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
        List<Job> jobs = jobRepository.findAll();
        BatchLookup lookup = buildLookup(jobs);
        return jobs.stream()
                .map(job -> toDto(job, lookup))
                .toList();
    }

    public JobSearchResponse findAllPage(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = safeSize(size);
        Page<Job> jobPage = jobRepository.findAll(PageRequest.of(safePage, safeSize));
        List<Job> jobs = jobPage.getContent();
        BatchLookup lookup = buildLookup(jobs);
        List<JobDto> dtos = jobs.stream()
                .map(job -> toDto(job, lookup))
                .toList();

        return new JobSearchResponse(
                dtos,
                jobPage.getTotalElements(),
                safePage,
                safeSize,
                jobPage.hasNext(),
                null,
                null
        );
    }

    public List<JobDto> search(String query) {
        if (query == null || query.isBlank()) {
            return findAll();
        }

        List<Job> jobs = jobRepository.findByJobNameContainingIgnoreCaseOrCompanyNameContainingIgnoreCase(query, query);
        BatchLookup lookup = buildLookup(jobs);
        return jobs.stream()
                .map(job -> toDto(job, lookup))
                .toList();
    }

    public List<JobDto> filter(JobFilterRequest request) {
        return filterResponse(request, 0, Integer.MAX_VALUE).jobs();
    }

    public JobSearchResponse filterResponse(JobFilterRequest request) {
        return filterResponse(request, DEFAULT_MIN_MATCH_SCORE, DEFAULT_MATCH_LIMIT);
    }

    public JobSearchResponse filterResponse(JobFilterRequest request, int minScore, int limit) {
        if (request == null) {
            return findAllPage(0, limit);
        }

        boolean hasCriteria = hasActiveCriteria(request);
        List<SkillRelation> skillRelations = isEmpty(request.skills()) ? List.of() : skillRelationRepository.findAll();
        List<Job> jobs = jobRepository.findAll();
        BatchLookup lookup = buildLookup(jobs);

        List<ScoredJob> scoredJobs = jobs.stream()
                .map(job -> new ScoredJob(job, calculateMatchScore(job, request, skillRelations, lookup)))
                .filter(scoredJob -> !hasCriteria || scoredJob.score().comparedFields() == 0
                        || scoredJob.score().matchedFields() > 0)
                .filter(scoredJob -> scoredJob.score().matchPercentage() >= minScore)
                .sorted(Comparator.comparingInt((ScoredJob scoredJob) -> scoredJob.score().matchPercentage()).reversed())
                .toList();

        int safeLimit = Math.max(1, limit);
        List<JobDto> page = scoredJobs.stream()
                .limit(safeLimit)
                .map(scoredJob -> toDto(
                        scoredJob.job(),
                        scoredJob.score().matchPercentage(),
                        scoredJob.score().confidencePercentage(),
                        lookup
                ))
                .toList();

        Integer averageMatch = scoredJobs.isEmpty()
                ? null
                : (int) Math.round(scoredJobs.stream()
                        .mapToInt(scoredJob -> scoredJob.score().matchPercentage())
                        .average()
                        .orElse(0.0));

        return new JobSearchResponse(
                page,
                scoredJobs.size(),
                0,
                safeLimit,
                scoredJobs.size() > safeLimit,
                averageMatch,
                request
        );
    }

    private JobDto toDto(Job job) {
        return toDto(job, 100, 0, buildLookup(List.of(job)));
    }

    private JobDto toDto(Job job, BatchLookup lookup) {
        return toDto(job, 100, 0, lookup);
    }

    private JobDto toDto(Job job, int matchScore, int confidenceScore, BatchLookup lookup) {
        List<String> skills = lookup.skillsByJobId().getOrDefault(job.getId(), List.of()).stream()
                .map(jobSkill -> jobSkill.getSkill().getName())
                .toList();

        String workMode = lookup.workTypesByJobId().getOrDefault(job.getId(), List.of()).stream()
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
                confidenceScore,
                toleranceLevel(matchScore),
                skills
        );
    }

    private MatchScore calculateMatchScore(Job job, JobFilterRequest request, List<SkillRelation> skillRelations, BatchLookup lookup) {
        MatchScore score = new MatchScore();
        JobFilterRequest.JobCriteria userCriteria = request.job();
        JobFilterRequest.LocationCriteria userLocation = request.location();

        // Job je osnova. Ce job nima tega podatka, se kriterij ne racuna.
        if (userCriteria != null) {
            score.addJobTextRequirement(job.getJobName(), userCriteria.jobname(), TITLE_WEIGHT);
            score.addJobExperienceRequirement(job.getRequiredExperience(), userCriteria.requiredExperience(), EXPERIENCE_WEIGHT);

            score.addJobEducationRequirement(
                    job.getEducationLevel() == null ? null : job.getEducationLevel().getId(),
                    job.getEducationLevel() == null ? null : job.getEducationLevel().getName(),
                    userCriteria.educationLevel(),
                    EDUCATION_WEIGHT
            );

            BigDecimal userMinSalary = firstNonNull(userCriteria.minSalary(), userCriteria.predictedMinSalary());
            BigDecimal userMaxSalary = firstNonNull(userCriteria.maxSalary(), userCriteria.predictedMaxSalary());
            BigDecimal jobMinSalary = firstNonNull(job.getMinSalary(), job.getPredictedMinSalary());
            BigDecimal jobMaxSalary = firstNonNull(job.getMaxSalary(), job.getPredictedMaxSalary());
            score.addJobSalaryRequirement(jobMinSalary, jobMaxSalary, userMinSalary, userMaxSalary, SALARY_WEIGHT);
        } else {
            score.addJobTextRequirement(job.getJobName(), null, TITLE_WEIGHT);
            score.addJobExperienceRequirement(job.getRequiredExperience(), null, EXPERIENCE_WEIGHT);

            score.addJobEducationRequirement(
                    job.getEducationLevel() == null ? null : job.getEducationLevel().getId(),
                    job.getEducationLevel() == null ? null : job.getEducationLevel().getName(),
                    null,
                    EDUCATION_WEIGHT
            );

            BigDecimal jobMinSalary = firstNonNull(job.getMinSalary(), job.getPredictedMinSalary());
            BigDecimal jobMaxSalary = firstNonNull(job.getMaxSalary(), job.getPredictedMaxSalary());
            score.addJobSalaryRequirement(jobMinSalary, jobMaxSalary, null, null, SALARY_WEIGHT);
        }

        addSkillScore(score, job, request.skills(), skillRelations, lookup);
        addWorkTypeScore(score, job, request.workTypes(), lookup);

        if (!isRemote(job, lookup)) {
            addLocationScore(score, job.getLocation(), userLocation);
        }

        return score;
    }

    private void addLocationScore(MatchScore score, Location jobLocation, JobFilterRequest.LocationCriteria userLocation) {
        if (jobLocation == null) {
            return;
        }

        List<Double> weights = new ArrayList<>();

        addJobTextWeight(weights, jobLocation.getCityDistrict(), userLocation == null ? null : userLocation.cityDistrict());
        addJobAnyTextWeight(weights, jobLocation.getCity(), userLocation == null ? null : userLocation.city(), userLocation == null ? null : userLocation.cities());
        addJobAnyTextWeight(weights, jobLocation.getRegion(), userLocation == null ? null : userLocation.region(), userLocation == null ? null : userLocation.regions());
        addJobAnyTextWeight(weights, jobLocation.getCountry(), userLocation == null ? null : userLocation.country(), userLocation == null ? null : userLocation.countries());
        addJobExactWeight(weights, jobLocation.getLatitude(), userLocation == null ? null : userLocation.latitude());
        addJobExactWeight(weights, jobLocation.getLongitude(), userLocation == null ? null : userLocation.longitude());

        if (weights.isEmpty()) {
            return;
        }

        double average = weights.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        score.addWeighted(LOCATION_WEIGHT, average);
    }

    private void addJobTextWeight(List<Double> weights, String jobValue, String userValue) {
        if (!hasText(jobValue)) {
            return;
        }

        weights.add(hasText(userValue) ? textMatchWeight(userValue, jobValue) : 0.0);
    }

    private void addJobAnyTextWeight(List<Double> weights, String jobValue, String singleUserValue, List<String> userValues) {
        if (!hasText(jobValue)) {
            return;
        }

        List<String> values = new ArrayList<>();

        if (hasText(singleUserValue)) {
            values.add(singleUserValue);
        }

        if (userValues != null) {
            userValues.stream()
                    .filter(this::hasText)
                    .forEach(values::add);
        }

        if (values.isEmpty()) {
            weights.add(0.0);
            return;
        }

        double best = values.stream()
                .mapToDouble(userValue -> textMatchWeight(userValue, jobValue))
                .max()
                .orElse(0.0);

        weights.add(best);
    }

    private void addJobExactWeight(List<Double> weights, Object jobValue, Object userValue) {
        if (jobValue == null) {
            return;
        }

        weights.add(userValue != null && jobValue.equals(userValue) ? 1.0 : 0.0);
    }

    private void addSkillScore(MatchScore score, Job job, List<String> userSkills, List<SkillRelation> skillRelations, BatchLookup lookup) {
        List<String> jobSkillValues = lookup.skillsByJobId().getOrDefault(job.getId(), List.of()).stream()
                .flatMap(jobSkill -> List.of(jobSkill.getSkill().getId(), jobSkill.getSkill().getName()).stream())
                .map(this::normalize)
                .toList();

        if (jobSkillValues.isEmpty()) {
            return;
        }

        List<String> userSkillValues = userSkills == null
                ? List.of()
                : userSkills.stream()
                        .filter(this::hasText)
                        .map(this::normalize)
                        .toList();

        // Job skills so zahteve. Extra user skills ne znizajo rezultata.
        List<Double> weights = jobSkillValues.stream()
                .map(jobSkill -> skillMatchWeight(jobSkill, userSkillValues, skillRelations))
                .toList();

        double average = weights.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        score.addWeighted(SKILL_WEIGHT, average);
    }

    private double skillMatchWeight(String jobSkill, List<String> userSkillValues, List<SkillRelation> relations) {
        if (userSkillValues.contains(jobSkill)) {
            return DIRECT_SKILL_MATCH;
        }

        return relations.stream()
                .mapToDouble(relation -> relationMatchWeight(jobSkill, userSkillValues, relation))
                .max()
                .orElse(0.0);
    }

    private double relationMatchWeight(String jobSkill, List<String> userSkillValues, SkillRelation relation) {
        String sourceId = normalize(relation.getSourceSkill().getId());
        String sourceName = normalize(relation.getSourceSkill().getName());
        String targetId = normalize(relation.getTargetSkill().getId());
        String targetName = normalize(relation.getTargetSkill().getName());

        boolean jobIsSource = jobSkill.equals(sourceId) || jobSkill.equals(sourceName);
        boolean jobIsTarget = jobSkill.equals(targetId) || jobSkill.equals(targetName);
        boolean userHasSource = userSkillValues.contains(sourceId) || userSkillValues.contains(sourceName);
        boolean userHasTarget = userSkillValues.contains(targetId) || userSkillValues.contains(targetName);

        if ((jobIsSource && userHasTarget) || (jobIsTarget && userHasSource)) {
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

    private void addWorkTypeScore(MatchScore score, Job job, List<String> userWorkTypes, BatchLookup lookup) {
        List<String> jobWorkTypes = jobWorkTypes(job, lookup);

        if (jobWorkTypes.isEmpty()) {
            return;
        }

        List<String> userValues = userWorkTypes == null
                ? List.of()
                : userWorkTypes.stream()
                        .filter(this::hasText)
                        .map(this::normalize)
                        .toList();

        if (userValues.isEmpty()) {
            score.addWeighted(WORK_TYPE_WEIGHT, 0.0);
            return;
        }

        // Ce job zahteva vec work type vrednosti, kandidat dobi povprecje po zahtevah joba.
        double average = jobWorkTypes.stream()
                .mapToDouble(jobWorkType -> userValues.stream()
                        .mapToDouble(userWorkType -> textMatchWeight(userWorkType, jobWorkType))
                        .max()
                        .orElse(0.0))
                .average()
                .orElse(0.0);

        score.addWeighted(WORK_TYPE_WEIGHT, average);
    }

    private boolean hasActiveCriteria(JobFilterRequest request) {
        return hasActiveJobCriteria(request.job())
                || hasActiveLocationCriteria(request.location())
                || !isEmpty(request.skills())
                || !isEmpty(request.workTypes());
    }

    private boolean hasActiveJobCriteria(JobFilterRequest.JobCriteria criteria) {
        return criteria != null && (hasText(criteria.jobname())
                || hasText(criteria.experienceLevelName())
                || hasText(criteria.educationLevel())
                || criteria.requiredExperience() != null
                || criteria.predictedMinSalary() != null
                || criteria.predictedMaxSalary() != null
                || criteria.minSalary() != null
                || criteria.maxSalary() != null);
    }

    private boolean hasActiveLocationCriteria(JobFilterRequest.LocationCriteria criteria) {
        return criteria != null && (hasText(criteria.cityDistrict())
                || hasText(criteria.city())
                || !isEmpty(criteria.cities())
                || hasText(criteria.region())
                || !isEmpty(criteria.regions())
                || hasText(criteria.country())
                || !isEmpty(criteria.countries())
                || criteria.latitude() != null
                || criteria.longitude() != null);
    }

    private boolean isRemote(Job job, BatchLookup lookup) {
        return jobWorkTypes(job, lookup).stream().anyMatch(workType -> workType.contains("remote"));
    }

    private List<String> jobWorkTypes(Job job, BatchLookup lookup) {
        return lookup.workTypesByJobId().getOrDefault(job.getId(), List.of()).stream()
                .flatMap(workTypeJob -> List.of(workTypeJob.getWorkType().getId(), workTypeJob.getWorkType().getName()).stream())
                .map(this::normalize)
                .toList();
    }

    private BatchLookup buildLookup(List<Job> jobs) {
        List<String> jobIds = jobs.stream().map(Job::getId).toList();
        if (jobIds.isEmpty()) {
            return new BatchLookup(Map.of(), Map.of());
        }

        Map<String, List<JobSkill>> skillsByJobId = jobSkillRepository.findByJob_IdIn(jobIds).stream()
                .filter(jobSkill -> jobSkill.getJob() != null)
                .collect(Collectors.groupingBy(jobSkill -> jobSkill.getJob().getId()));

        Map<String, List<WorkTypeJob>> workTypesByJobId = workTypeJobRepository.findByJob_IdIn(jobIds).stream()
                .filter(workTypeJob -> workTypeJob.getJob() != null)
                .collect(Collectors.groupingBy(workTypeJob -> workTypeJob.getJob().getId()));

        return new BatchLookup(skillsByJobId, workTypesByJobId);
    }

    private BigDecimal firstNonNull(BigDecimal first, BigDecimal second) {
        return first != null ? first : second;
    }

    private int safeSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
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
                .replace("Ãƒâ€žÃ‚Â", "c")
                .replace("Ãƒâ€žÃ¢â‚¬Â¡", "c")
                .replace("Ãƒâ€¦Ã‚Â¡", "s")
                .replace("Ãƒâ€¦Ã‚Â¾", "z")
                .replace("Ãƒâ€žÃ¢â‚¬Ëœ", "dj")
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

    private record BatchLookup(
            Map<String, List<JobSkill>> skillsByJobId,
            Map<String, List<WorkTypeJob>> workTypesByJobId
    ) {
    }

    private class MatchScore {
        private double matchedFields;
        private double comparedFields;

        void addWeighted(double fieldWeight, double matchWeight) {
            comparedFields += fieldWeight;
            matchedFields += fieldWeight * Math.max(0.0, Math.min(1.0, matchWeight));
        }

        void addJobTextRequirement(String jobValue, String userValue, double fieldWeight) {
            if (!hasText(jobValue)) {
                return;
            }

            addWeighted(fieldWeight, hasText(userValue) ? textMatchWeight(userValue, jobValue) : 0.0);
        }

        void addJobEducationRequirement(String jobEducationId, String jobEducationName, String userEducation, double fieldWeight) {
            if (!hasText(jobEducationId) && !hasText(jobEducationName)) {
                return;
            }

            if (!hasText(userEducation)) {
                addWeighted(fieldWeight, 0.0);
                return;
            }

            String normalizedUserEducation = normalize(userEducation);
            boolean idMatches = hasText(jobEducationId) && normalize(jobEducationId).equals(normalizedUserEducation);
            double nameWeight = hasText(jobEducationName) ? textMatchWeight(userEducation, jobEducationName) : 0.0;
            addWeighted(fieldWeight, idMatches ? 1.0 : nameWeight);
        }

        void addJobSalaryRequirement(BigDecimal jobMin, BigDecimal jobMax, BigDecimal userMin, BigDecimal userMax, double fieldWeight) {
            if (jobMin == null && jobMax == null) {
                return;
            }

            if (userMin == null && userMax == null) {
                addWeighted(fieldWeight, 0.0);
                return;
            }

            BigDecimal min = jobMin == null ? jobMax : jobMin;
            BigDecimal max = jobMax == null ? jobMin : jobMax;

            boolean minMatches = userMin == null || max.compareTo(userMin) >= 0;
            boolean maxMatches = userMax == null || min.compareTo(userMax) <= 0;
            addWeighted(fieldWeight, minMatches && maxMatches ? 1.0 : 0.0);
        }

        void addJobExperienceRequirement(Integer jobRequiredExperience, Integer userExperience, double fieldWeight) {
            if (jobRequiredExperience == null) {
                return;
            }

            if (userExperience == null) {
                addWeighted(fieldWeight, 0.0);
                return;
            }

            int requiredMonths = toMonths(jobRequiredExperience);
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

        int matchPercentage() {
            if (comparedFields == 0) {
                return 100;
            }

            return (int) Math.round((matchedFields * 100.0) / comparedFields);
        }

        int confidencePercentage() {
            return (int) Math.round(Math.max(0.0, Math.min(100.0, comparedFields)));
        }

        private int toMonths(Integer value) {
            return value <= 10 ? value * 12 : value;
        }
    }
}