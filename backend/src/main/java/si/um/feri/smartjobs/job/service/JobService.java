package si.um.feri.smartjobs.job.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import si.um.feri.smartjobs.analytics.dto.AnalyticsDashboardDto;
import si.um.feri.smartjobs.analytics.dto.AnalyticsSummaryDto;
import si.um.feri.smartjobs.analytics.dto.CountStatDto;
import si.um.feri.smartjobs.analytics.dto.LocationStatDto;
import si.um.feri.smartjobs.analytics.dto.SalaryStatsDto;
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
    private static final double TITLE_WEIGHT = 15.0;
    private static final double EXPERIENCE_WEIGHT = 10.0;
    private static final double LOCATION_WEIGHT = 20.0;
    private static final double WORK_TYPE_WEIGHT = 7.0;
    private static final double EDUCATION_WEIGHT = 5.0;
    private static final double SALARY_WEIGHT = 3.0;

    public static final int DEFAULT_PAGE_SIZE = 50;
    public static final int MAX_PAGE_SIZE = 200;
    public static final int DEFAULT_MATCH_LIMIT = 200;
    public static final int DEFAULT_MIN_MATCH_SCORE = 30;
    private static final int FALLBACK_MIN_MATCH_SCORE = 2;
    private static final int MIN_RESULTS_BEFORE_SCORE_FALLBACK = 50;

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
    private volatile SkillRelationIndex skillRelationIndex = SkillRelationIndex.empty();
    private volatile JobLookupIndex jobLookupIndex = JobLookupIndex.empty();

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

    @PostConstruct
    public void refreshSkillRelationIndexOnStartup() {
        refreshSkillRelationIndex();
        refreshJobLookupIndex();
    }

    @Scheduled(cron = "0 10 3 * * MON", zone = "Europe/Ljubljana")
    public void refreshSkillRelationIndexWeekly() {
        refreshSkillRelationIndex();
        refreshJobLookupIndex();
    }

    public void refreshSkillRelationIndex() {
        skillRelationIndex = buildSkillRelationIndex(skillRelationRepository.findAll());
    }

    public void refreshJobLookupIndex() {
        jobLookupIndex = buildJobLookupIndex(
                jobSkillRepository.findAll(),
                workTypeJobRepository.findAll()
        );
    }

    public boolean isSkillRelationIndexLoaded() {
        return !skillRelationIndex.relatedBySkill().isEmpty();
    }

    public boolean isJobLookupIndexLoaded() {
        return !jobLookupIndex.skillsByJobId().isEmpty() || !jobLookupIndex.workTypesByJobId().isEmpty();
    }

    public Map<String, Integer> cacheSizes() {
        return Map.of(
                "skillRelationKeys", skillRelationIndex.relatedBySkill().size(),
                "jobSkillLookups", jobLookupIndex.skillsByJobId().size(),
                "jobWorkTypeLookups", jobLookupIndex.workTypesByJobId().size()
        );
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
        SkillRelationIndex activeSkillRelationIndex = isEmpty(request.skills())
                ? SkillRelationIndex.empty()
                : skillRelationIndex;
        List<Job> jobs = findCandidateJobs(request, activeSkillRelationIndex);
        BatchLookup lookup = buildLookup(jobs);

        List<ScoredJob> scoredJobs = jobs.stream()
                .map(job -> new ScoredJob(job, calculateMatchScore(job, request, activeSkillRelationIndex, lookup)))
                .filter(scoredJob -> !hasCriteria || scoredJob.score().comparedFields() == 0
                        || scoredJob.score().matchedFields() > 0)
                .filter(scoredJob -> scoredJob.score().matchPercentage() >= minScore)
                .sorted(Comparator.comparingInt((ScoredJob scoredJob) -> scoredJob.score().matchPercentage()).reversed())
                .toList();

        if (minScore > FALLBACK_MIN_MATCH_SCORE && scoredJobs.size() < MIN_RESULTS_BEFORE_SCORE_FALLBACK) {
            scoredJobs = jobs.stream()
                    .map(job -> new ScoredJob(job, calculateMatchScore(job, request, activeSkillRelationIndex, lookup)))
                    .filter(scoredJob -> !hasCriteria || scoredJob.score().comparedFields() == 0
                            || scoredJob.score().matchedFields() > 0)
                    .filter(scoredJob -> scoredJob.score().matchPercentage() >= FALLBACK_MIN_MATCH_SCORE)
                    .sorted(Comparator.comparingInt((ScoredJob scoredJob) -> scoredJob.score().matchPercentage()).reversed())
                    .toList();
        }

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
                request,
                buildFilteredAnalytics(scoredJobs, lookup, averageMatch)
        );
    }

    private AnalyticsDashboardDto buildFilteredAnalytics(List<ScoredJob> scoredJobs, BatchLookup lookup, Integer averageMatch) {
        List<Job> jobs = scoredJobs.stream().map(ScoredJob::job).toList();
        long totalJobs = jobs.size();
        SalaryStatsDto salaryStats = salaryStats(jobs);

        AnalyticsSummaryDto summary = new AnalyticsSummaryDto(
                totalJobs,
                jobs.stream()
                        .map(Job::getCompanyName)
                        .filter(this::hasText)
                        .map(this::normalize)
                        .distinct()
                        .count(),
                jobs.stream()
                        .map(Job::getLocation)
                        .filter(Objects::nonNull)
                        .map(Location::getId)
                        .distinct()
                        .count(),
                jobs.stream()
                        .map(Job::getLocation)
                        .filter(Objects::nonNull)
                        .map(Location::getCountry)
                        .filter(this::hasText)
                        .map(this::normalize)
                        .distinct()
                        .count(),
                salaryStats.jobsWithSalary(),
                jobs.stream().filter(job -> isRemote(job, lookup)).count(),
                salaryStats.averageSalary(),
                salaryStats.highestSalary(),
                averageMatch
        );

        return new AnalyticsDashboardDto(
                summary,
                topSkills(jobs, lookup, totalJobs, 50),
                skillTypeStats(jobs, lookup, totalJobs, 50),
                locationStats(jobs, LocationLevel.CITY, 50),
                locationStats(jobs, LocationLevel.REGION, 50),
                locationStats(jobs, LocationLevel.COUNTRY, 50),
                countBy(jobs, job -> job.getExperienceLevel() == null ? "Unknown" : job.getExperienceLevel().getName(), totalJobs, 50),
                workTypeStats(jobs, lookup, totalJobs, 50),
                countBy(jobs, job -> job.getEducationLevel() == null ? "Unknown" : job.getEducationLevel().getName(), totalJobs, 50),
                countBy(jobs, job -> hasText(job.getSourceWebsite()) ? job.getSourceWebsite() : "Unknown", totalJobs, 50),
                salaryStats
        );
    }

    private JobDto toDto(Job job) {
        return toDto(job, 100, 0, buildLookup(List.of(job)));
    }

    private JobDto toDto(Job job, BatchLookup lookup) {
        return toDto(job, 100, 0, lookup);
    }

    private JobDto toDto(Job job, int matchScore, int confidenceScore, BatchLookup lookup) {
        List<String> skills = lookup.skillsByJobId()
                .getOrDefault(job.getId(), SkillValues.empty())
                .displayNames();

        String workMode = lookup.workTypesByJobId()
                .getOrDefault(job.getId(), WorkTypeValues.empty())
                .displayNames()
                .stream()
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

    private MatchScore calculateMatchScore(Job job, JobFilterRequest request, SkillRelationIndex skillRelationIndex, BatchLookup lookup) {
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

        addSkillScore(score, job, request.skills(), skillRelationIndex, lookup);
        addWorkTypeScore(score, job, request.workTypes(), lookup);

        if (!isRemote(job, lookup)) {
            addLocationScore(score, job.getLocation(), userLocation);
        }

        return score;
    }

    private void addLocationScore(MatchScore score, Location jobLocation, JobFilterRequest.LocationCriteria userLocation) {
        if (jobLocation == null || !hasActiveLocationCriteria(userLocation)) {
            return;
        }

        List<Double> weights = new ArrayList<>();

        if (hasCityLocationCriteria(userLocation)) {
            addJobTextWeight(weights, jobLocation.getCityDistrict(), userLocation.cityDistrict());
            addJobAnyTextWeight(weights, jobLocation.getCity(), userLocation.city(), userLocation.cities());
        } else if (hasRegionLocationCriteria(userLocation)) {
            addJobAnyTextWeight(weights, jobLocation.getRegion(), userLocation.region(), userLocation.regions());
        } else {
            addJobAnyTextWeight(weights, jobLocation.getCountry(), userLocation.country(), userLocation.countries());
        }

        if (weights.isEmpty()) {
            return;
        }

        double average = weights.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        score.addWeighted(LOCATION_WEIGHT, average);
    }

    private void addJobTextWeight(List<Double> weights, String jobValue, String userValue) {
        if (!hasText(jobValue) || !hasText(userValue)) {
            return;
        }

        weights.add(textMatchWeight(userValue, jobValue));
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
            return;
        }

        double best = values.stream()
                .mapToDouble(userValue -> textMatchWeight(userValue, jobValue))
                .max()
                .orElse(0.0);

        weights.add(best);
    }

    private void addJobExactWeight(List<Double> weights, Object jobValue, Object userValue) {
        if (jobValue == null || userValue == null) {
            return;
        }

        weights.add(jobValue.equals(userValue) ? 1.0 : 0.0);
    }

    private void addSkillScore(MatchScore score, Job job, List<String> userSkills, SkillRelationIndex skillRelationIndex, BatchLookup lookup) {
        List<String> jobSkillValues = lookup.skillsByJobId()
                .getOrDefault(job.getId(), SkillValues.empty())
                .normalizedValues();

        if (jobSkillValues.isEmpty()) {
            return;
        }

        List<String> userSkillValues = userSkills == null
                ? List.of()
                : userSkills.stream()
                        .filter(this::hasText)
                        .map(this::normalize)
                        .distinct()
                        .toList();

        if (userSkillValues.isEmpty()) {
            score.addWeighted(SKILL_WEIGHT, 0.0);
            return;
        }

        Set<String> userSkillValueSet = new LinkedHashSet<>(userSkillValues);
        Set<String> jobSkillValueSet = new LinkedHashSet<>(jobSkillValues);

        // Main part: how many of the skills requested by the user are covered by this job?
        // Exact matches count as 1.0. Related skills use the relationship weight from SkillRelationIndex.
        // Example: user wants Teaching + Biology.
        // Job with Teaching + Biology scores much higher than job with only Teaching.
        double userSkillCoverage = userSkillValues.stream()
                .mapToDouble(userSkill -> skillMatchWeight(userSkill, jobSkillValueSet, skillRelationIndex))
                .average()
                .orElse(0.0);

        // Smaller extra part: reward jobs whose own skills are also relevant to the user.
        // This keeps related/specialized job skills useful, but prevents one broad skill from dominating.
        double jobSkillCoverage = jobSkillValues.stream()
                .mapToDouble(jobSkill -> skillMatchWeight(jobSkill, userSkillValueSet, skillRelationIndex))
                .average()
                .orElse(0.0);

        double average = userSkillCoverage * 0.85 + jobSkillCoverage * 0.15;
        score.addWeighted(SKILL_WEIGHT, average);
    }

    private double skillMatchWeight(String jobSkill, Set<String> userSkillValues, SkillRelationIndex skillRelationIndex) {
        if (userSkillValues.contains(jobSkill)) {
            return DIRECT_SKILL_MATCH;
        }

        Map<String, Double> relatedSkills = skillRelationIndex.relatedBySkill().get(jobSkill);
        if (relatedSkills == null || relatedSkills.isEmpty()) {
            return 0.0;
        }

        return userSkillValues.stream()
                .map(relatedSkills::get)
                .filter(weight -> weight != null)
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);
    }

    private double relationWeight(String relationshipType) {
        return switch ((relationshipType == null ? "" : relationshipType).toUpperCase(Locale.ROOT)) {
            case "FRAMEWORK_OF", "IMPLEMENTATION_OF" -> 0.8;
            case "SPECIALIZATION_OF" -> 0.7;
            case "PART_OF", "TOOL_FOR", "USED_WITH" -> 0.6;
            case "REQUIRES" -> 0.5;
            case "RELATED_TO", "SUPPORTS", "DATABASE_TECHNOLOGY" -> 0.4;
            default -> 0.4;
        };
    }

    private SkillRelationIndex buildSkillRelationIndex(List<SkillRelation> relations) {
        Map<String, Map<String, Double>> relatedBySkill = new HashMap<>();

        for (SkillRelation relation : relations) {
            if (relation.getSourceSkill() == null || relation.getTargetSkill() == null) {
                continue;
            }

            Set<String> sourceValues = normalizedSkillValues(
                    relation.getSourceSkill().getId(),
                    relation.getSourceSkill().getName()
            );
            Set<String> targetValues = normalizedSkillValues(
                    relation.getTargetSkill().getId(),
                    relation.getTargetSkill().getName()
            );
            double weight = relationWeight(relation.getRelationshipType());

            addRelatedSkillValues(relatedBySkill, sourceValues, targetValues, weight);
            addRelatedSkillValues(relatedBySkill, targetValues, sourceValues, weight);
        }

        return new SkillRelationIndex(relatedBySkill);
    }

    private void addRelatedSkillValues(
            Map<String, Map<String, Double>> relatedBySkill,
            Set<String> fromValues,
            Set<String> toValues,
            double weight
    ) {
        for (String from : fromValues) {
            Map<String, Double> related = relatedBySkill.computeIfAbsent(from, ignored -> new HashMap<>());
            for (String to : toValues) {
                related.merge(to, weight, Math::max);
            }
        }
    }

    private Set<String> normalizedSkillValues(String id, String name) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (hasText(id)) {
            values.add(normalize(id));
        }
        if (hasText(name)) {
            values.add(normalize(name));
        }
        return values;
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

    private List<Job> findCandidateJobs(JobFilterRequest request, SkillRelationIndex skillRelationIndex) {
        if (!hasActiveCriteria(request)) {
            return jobRepository.findAll();
        }

        LinkedHashSet<String> candidateIds = new LinkedHashSet<>();

        addSkillCandidateIds(candidateIds, request.skills(), skillRelationIndex);
        addWorkTypeCandidateIds(candidateIds, request.workTypes());

        boolean hasLocationCriteria = hasActiveLocationCriteria(request.location());
        LinkedHashSet<String> locationCandidateIds = findLocationCandidateIds(request.location());
        if (hasLocationCriteria) {
            if (locationCandidateIds.isEmpty()) {
                return List.of();
            }
            if (candidateIds.isEmpty()) {
                candidateIds.addAll(locationCandidateIds);
            } else {
                candidateIds.retainAll(locationCandidateIds);
            }
        }

        if (candidateIds.isEmpty()) {
            if (hasLocationCriteria) {
                return List.of();
            }
            return jobRepository.findAll();
        }

        return jobRepository.findByIdIn(candidateIds);
    }

    private void addSkillCandidateIds(LinkedHashSet<String> candidateIds, List<String> skills, SkillRelationIndex skillRelationIndex) {
        if (isEmpty(skills)) {
            return;
        }

        LinkedHashSet<String> skillNames = skills.stream()
                .filter(this::hasText)
                .map(String::trim)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

        LinkedHashSet<String> normalizedValues = skillNames.stream()
                .map(this::normalize)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

        skillNames.addAll(skillRelationIndex.relatedValues(normalizedValues));
        normalizedValues.addAll(skillRelationIndex.relatedValues(normalizedValues));

        jobSkillRepository.findBySkill_NameIn(skillNames).stream()
                .map(jobSkill -> jobSkill.getJob() == null ? null : jobSkill.getJob().getId())
                .filter(this::hasText)
                .forEach(candidateIds::add);

        jobSkillRepository.findBySkill_IdIn(normalizedValues).stream()
                .map(jobSkill -> jobSkill.getJob() == null ? null : jobSkill.getJob().getId())
                .filter(this::hasText)
                .forEach(candidateIds::add);
    }

    private void addWorkTypeCandidateIds(LinkedHashSet<String> candidateIds, List<String> workTypes) {
        if (isEmpty(workTypes)) {
            return;
        }

        LinkedHashSet<String> values = workTypes.stream()
                .filter(this::hasText)
                .map(String::trim)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

        workTypeJobRepository.findByWorkType_NameIn(values).stream()
                .map(workTypeJob -> workTypeJob.getJob() == null ? null : workTypeJob.getJob().getId())
                .filter(this::hasText)
                .forEach(candidateIds::add);
    }

    private LinkedHashSet<String> findLocationCandidateIds(JobFilterRequest.LocationCriteria location) {
        LinkedHashSet<String> candidateIds = new LinkedHashSet<>();
        if (location == null) {
            return candidateIds;
        }

        LinkedHashSet<String> cities = new LinkedHashSet<>();
        if (hasText(location.city())) {
            cities.add(location.city());
        }
        if (location.cities() != null) {
            location.cities().stream().filter(this::hasText).forEach(cities::add);
        }
        cities.stream()
                .flatMap(city -> jobRepository.findByLocation_CityContainingIgnoreCase(city).stream())
                .map(Job::getId)
                .forEach(candidateIds::add);

        if (!cities.isEmpty()) {
            return candidateIds;
        }

        LinkedHashSet<String> regions = new LinkedHashSet<>();
        if (hasText(location.region())) {
            regions.add(location.region());
        }
        if (location.regions() != null) {
            location.regions().stream().filter(this::hasText).forEach(regions::add);
        }
        regions.stream()
                .flatMap(region -> jobRepository.findByLocation_RegionContainingIgnoreCase(region).stream())
                .map(Job::getId)
                .forEach(candidateIds::add);

        if (!regions.isEmpty()) {
            return candidateIds;
        }

        LinkedHashSet<String> countries = new LinkedHashSet<>();
        if (hasText(location.country())) {
            countries.add(location.country());
        }
        if (location.countries() != null) {
            location.countries().stream().filter(this::hasText).forEach(countries::add);
        }
        countries.stream()
                .flatMap(country -> jobRepository.findByLocation_CountryContainingIgnoreCase(country).stream())
                .map(Job::getId)
                .forEach(candidateIds::add);

        return candidateIds;
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

    private boolean hasCityLocationCriteria(JobFilterRequest.LocationCriteria criteria) {
        return criteria != null && (hasText(criteria.cityDistrict())
                || hasText(criteria.city())
                || !isEmpty(criteria.cities())
                || criteria.latitude() != null
                || criteria.longitude() != null);
    }

    private boolean hasRegionLocationCriteria(JobFilterRequest.LocationCriteria criteria) {
        return criteria != null && (hasText(criteria.region())
                || !isEmpty(criteria.regions()));
    }

    private boolean isRemote(Job job, BatchLookup lookup) {
        return jobWorkTypes(job, lookup).stream().anyMatch(workType -> workType.contains("remote"));
    }

    private List<String> jobWorkTypes(Job job, BatchLookup lookup) {
        return lookup.workTypesByJobId()
                .getOrDefault(job.getId(), WorkTypeValues.empty())
                .normalizedValues();
    }

    private BatchLookup buildLookup(List<Job> jobs) {
        List<String> jobIds = jobs.stream().map(Job::getId).toList();
        if (jobIds.isEmpty()) {
            return new BatchLookup(Map.of(), Map.of());
        }

        JobLookupIndex index = jobLookupIndex;
        Map<String, SkillValues> skillsByJobId = new HashMap<>();
        Map<String, WorkTypeValues> workTypesByJobId = new HashMap<>();
        List<String> missingSkillJobIds = new ArrayList<>();
        List<String> missingWorkTypeJobIds = new ArrayList<>();

        for (String jobId : jobIds) {
            SkillValues skillValues = index.skillsByJobId().get(jobId);
            if (skillValues == null) {
                missingSkillJobIds.add(jobId);
            } else {
                skillsByJobId.put(jobId, skillValues);
            }

            WorkTypeValues workTypeValues = index.workTypesByJobId().get(jobId);
            if (workTypeValues == null) {
                missingWorkTypeJobIds.add(jobId);
            } else {
                workTypesByJobId.put(jobId, workTypeValues);
            }
        }

        if (!missingSkillJobIds.isEmpty()) {
            skillsByJobId.putAll(skillValuesByJobId(jobSkillRepository.findByJob_IdIn(missingSkillJobIds)));
        }

        if (!missingWorkTypeJobIds.isEmpty()) {
            workTypesByJobId.putAll(workTypeValuesByJobId(workTypeJobRepository.findByJob_IdIn(missingWorkTypeJobIds)));
        }

        return new BatchLookup(skillsByJobId, workTypesByJobId);
    }

    private JobLookupIndex buildJobLookupIndex(List<JobSkill> jobSkills, List<WorkTypeJob> workTypeJobs) {
        return new JobLookupIndex(
                skillValuesByJobId(jobSkills),
                workTypeValuesByJobId(workTypeJobs)
        );
    }

    private Map<String, SkillValues> skillValuesByJobId(List<JobSkill> jobSkills) {
        return jobSkills.stream()
                .filter(jobSkill -> jobSkill.getJob() != null && jobSkill.getSkill() != null)
                .collect(Collectors.groupingBy(
                        jobSkill -> jobSkill.getJob().getId(),
                        Collectors.collectingAndThen(Collectors.toList(), this::toSkillValues)
                ));
    }

    private SkillValues toSkillValues(List<JobSkill> jobSkills) {
        LinkedHashSet<String> displayNames = new LinkedHashSet<>();
        LinkedHashSet<String> normalizedValues = new LinkedHashSet<>();
        LinkedHashSet<String> skillTypeNames = new LinkedHashSet<>();

        for (JobSkill jobSkill : jobSkills) {
            if (jobSkill.getSkill() == null) {
                continue;
            }
            if (hasText(jobSkill.getSkill().getName())) {
                displayNames.add(jobSkill.getSkill().getName());
                normalizedValues.add(normalize(jobSkill.getSkill().getName()));
            }
            if (hasText(jobSkill.getSkill().getId())) {
                normalizedValues.add(normalize(jobSkill.getSkill().getId()));
            }
            if (jobSkill.getSkill().getSkillType() != null && hasText(jobSkill.getSkill().getSkillType().getName())) {
                skillTypeNames.add(jobSkill.getSkill().getSkillType().getName());
            }
        }

        return new SkillValues(new ArrayList<>(displayNames), new ArrayList<>(normalizedValues), new ArrayList<>(skillTypeNames));
    }

    private Map<String, WorkTypeValues> workTypeValuesByJobId(List<WorkTypeJob> workTypeJobs) {
        return workTypeJobs.stream()
                .filter(workTypeJob -> workTypeJob.getJob() != null && workTypeJob.getWorkType() != null)
                .collect(Collectors.groupingBy(
                        workTypeJob -> workTypeJob.getJob().getId(),
                        Collectors.collectingAndThen(Collectors.toList(), this::toWorkTypeValues)
                ));
    }

    private WorkTypeValues toWorkTypeValues(List<WorkTypeJob> workTypeJobs) {
        LinkedHashSet<String> displayNames = new LinkedHashSet<>();
        LinkedHashSet<String> normalizedValues = new LinkedHashSet<>();

        for (WorkTypeJob workTypeJob : workTypeJobs) {
            if (workTypeJob.getWorkType() == null) {
                continue;
            }
            if (hasText(workTypeJob.getWorkType().getName())) {
                displayNames.add(workTypeJob.getWorkType().getName());
                normalizedValues.add(normalize(workTypeJob.getWorkType().getName()));
            }
            if (hasText(workTypeJob.getWorkType().getId())) {
                normalizedValues.add(normalize(workTypeJob.getWorkType().getId()));
            }
        }

        return new WorkTypeValues(new ArrayList<>(displayNames), new ArrayList<>(normalizedValues));
    }

    private List<CountStatDto> topSkills(List<Job> jobs, BatchLookup lookup, long denominator, int limit) {
        return jobs.stream()
                .flatMap(job -> lookup.skillsByJobId()
                        .getOrDefault(job.getId(), SkillValues.empty())
                        .displayNames()
                        .stream())
                .filter(this::hasText)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .map(entry -> new CountStatDto(entry.getKey(), entry.getValue(), percentage(entry.getValue(), denominator)))
                .sorted(countComparator())
                .limit(safeAnalyticsLimit(limit))
                .toList();
    }

    private List<CountStatDto> skillTypeStats(List<Job> jobs, BatchLookup lookup, long denominator, int limit) {
        return jobs.stream()
                .flatMap(job -> lookup.skillsByJobId()
                        .getOrDefault(job.getId(), SkillValues.empty())
                        .skillTypeNames()
                        .stream()
                        .distinct())
                .filter(this::hasText)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .map(entry -> new CountStatDto(entry.getKey(), entry.getValue(), percentage(entry.getValue(), denominator)))
                .sorted(countComparator())
                .limit(safeAnalyticsLimit(limit))
                .toList();
    }

    private List<CountStatDto> workTypeStats(List<Job> jobs, BatchLookup lookup, long denominator, int limit) {
        return jobs.stream()
                .flatMap(job -> lookup.workTypesByJobId()
                        .getOrDefault(job.getId(), WorkTypeValues.empty())
                        .displayNames()
                        .stream())
                .filter(this::hasText)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .map(entry -> new CountStatDto(entry.getKey(), entry.getValue(), percentage(entry.getValue(), denominator)))
                .sorted(countComparator())
                .limit(safeAnalyticsLimit(limit))
                .toList();
    }

    private List<CountStatDto> countBy(List<Job> jobs, Function<Job, String> classifier, long denominator, int limit) {
        return jobs.stream()
                .map(classifier)
                .filter(this::hasText)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .map(entry -> new CountStatDto(entry.getKey(), entry.getValue(), percentage(entry.getValue(), denominator)))
                .sorted(countComparator())
                .limit(safeAnalyticsLimit(limit))
                .toList();
    }

    private List<LocationStatDto> locationStats(List<Job> jobs, LocationLevel level, int limit) {
        long totalJobs = jobs.size();

        return jobs.stream()
                .filter(job -> job.getLocation() != null)
                .collect(Collectors.groupingBy(job -> locationKey(job.getLocation(), level)))
                .entrySet()
                .stream()
                .filter(entry -> hasText(entry.getKey()))
                .map(entry -> toLocationStat(entry.getKey(), entry.getValue(), totalJobs, level))
                .sorted(Comparator.comparingLong(LocationStatDto::count).reversed()
                        .thenComparing(LocationStatDto::label))
                .limit(safeAnalyticsLimit(limit))
                .toList();
    }

    private LocationStatDto toLocationStat(String label, List<Job> jobs, long totalJobs, LocationLevel level) {
        Location sample = jobs.stream()
                .map(Job::getLocation)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        return new LocationStatDto(
                label,
                level == LocationLevel.CITY && sample != null ? sample.getCity() : null,
                sample == null ? null : sample.getRegion(),
                sample == null ? null : sample.getCountry(),
                jobs.size(),
                percentage(jobs.size(), totalJobs),
                averageCoordinate(jobs, true),
                averageCoordinate(jobs, false)
        );
    }

    private String locationKey(Location location, LocationLevel level) {
        return switch (level) {
            case CITY -> firstText(location.getCity(), location.getRegion(), location.getCountry(), "Unknown");
            case REGION -> firstText(location.getRegion(), location.getCountry(), "Unknown");
            case COUNTRY -> firstText(location.getCountry(), "Unknown");
        };
    }

    private BigDecimal averageCoordinate(List<Job> jobs, boolean latitude) {
        List<BigDecimal> coordinates = jobs.stream()
                .map(Job::getLocation)
                .filter(Objects::nonNull)
                .map(location -> latitude ? location.getLatitude() : location.getLongitude())
                .filter(Objects::nonNull)
                .toList();

        return average(coordinates, 6);
    }

    private SalaryStatsDto salaryStats(List<Job> jobs) {
        List<SalaryRange> salaryRanges = jobs.stream()
                .map(this::salaryRange)
                .flatMap(List::stream)
                .toList();

        if (salaryRanges.isEmpty()) {
            return new SalaryStatsDto(0, null, null, null, null, null);
        }

        BigDecimal lowest = salaryRanges.stream()
                .map(SalaryRange::min)
                .min(BigDecimal::compareTo)
                .orElse(null);
        BigDecimal highest = salaryRanges.stream()
                .map(SalaryRange::max)
                .max(BigDecimal::compareTo)
                .orElse(null);

        return new SalaryStatsDto(
                salaryRanges.size(),
                lowest,
                highest,
                average(salaryRanges.stream().map(SalaryRange::min).toList(), 2),
                average(salaryRanges.stream().map(SalaryRange::max).toList(), 2),
                average(salaryRanges.stream().map(SalaryRange::midpoint).toList(), 2)
        );
    }

    private List<SalaryRange> salaryRange(Job job) {
        BigDecimal min = firstNonNull(job.getMinSalary(), job.getPredictedMinSalary());
        BigDecimal max = firstNonNull(job.getMaxSalary(), job.getPredictedMaxSalary());
        min = firstNonNull(min, max);
        max = firstNonNull(max, min);

        if (min == null || max == null) {
            return List.of();
        }

        if (min.compareTo(max) > 0) {
            return List.of(new SalaryRange(max, min));
        }

        return List.of(new SalaryRange(min, max));
    }

    private BigDecimal average(List<BigDecimal> values, int scale) {
        List<BigDecimal> cleanValues = values.stream()
                .filter(Objects::nonNull)
                .toList();

        if (cleanValues.isEmpty()) {
            return null;
        }

        BigDecimal sum = cleanValues.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(cleanValues.size()), scale, RoundingMode.HALF_UP);
    }

    private Comparator<CountStatDto> countComparator() {
        return Comparator.comparingLong(CountStatDto::count)
                .reversed()
                .thenComparing(CountStatDto::label);
    }

    private double percentage(long count, long total) {
        if (total <= 0) {
            return 0;
        }

        return BigDecimal.valueOf(count * 100.0 / total)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private long safeAnalyticsLimit(int limit) {
        if (limit <= 0) {
            return 10;
        }

        return Math.min(limit, 50);
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }

        return "Unknown";
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

    private record SalaryRange(BigDecimal min, BigDecimal max) {
        BigDecimal midpoint() {
            return min.add(max).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        }
    }

    private enum LocationLevel {
        CITY,
        REGION,
        COUNTRY
    }

    private record BatchLookup(
            Map<String, SkillValues> skillsByJobId,
            Map<String, WorkTypeValues> workTypesByJobId
    ) {
    }

    private record JobLookupIndex(
            Map<String, SkillValues> skillsByJobId,
            Map<String, WorkTypeValues> workTypesByJobId
    ) {
        static JobLookupIndex empty() {
            return new JobLookupIndex(Map.of(), Map.of());
        }
    }

    private record SkillValues(List<String> displayNames, List<String> normalizedValues, List<String> skillTypeNames) {
        static SkillValues empty() {
            return new SkillValues(List.of(), List.of(), List.of());
        }
    }

    private record WorkTypeValues(List<String> displayNames, List<String> normalizedValues) {
        static WorkTypeValues empty() {
            return new WorkTypeValues(List.of(), List.of());
        }
    }

    private record SkillRelationIndex(Map<String, Map<String, Double>> relatedBySkill) {
        static SkillRelationIndex empty() {
            return new SkillRelationIndex(Map.of());
        }

        Set<String> relatedValues(Set<String> skillValues) {
            LinkedHashSet<String> result = new LinkedHashSet<>();
            for (String skillValue : skillValues) {
                Map<String, Double> related = relatedBySkill.get(skillValue);
                if (related != null) {
                    result.addAll(related.keySet());
                }
            }
            return result;
        }
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
