package si.um.feri.smartjobs.job.service;

import org.springframework.stereotype.Service;
import si.um.feri.smartjobs.job.dto.JobDto;
import si.um.feri.smartjobs.job.dto.JobFilterRequest;
import si.um.feri.smartjobs.job.entity.Job;
import si.um.feri.smartjobs.job.repository.JobRepository;
import si.um.feri.smartjobs.jobSkill.repository.JobSkillRepository;
import si.um.feri.smartjobs.location.entity.Location;
import si.um.feri.smartjobs.workTypeJob.repository.WorkTypeJobRepository;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
public class JobService {
    private static final double EXPERIENCE_TOLERANCE = 0.75;

    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final WorkTypeJobRepository workTypeJobRepository;

    public JobService(
            JobRepository jobRepository,
            JobSkillRepository jobSkillRepository,
            WorkTypeJobRepository workTypeJobRepository
    ) {
        this.jobRepository = jobRepository;
        this.jobSkillRepository = jobSkillRepository;
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

        return jobRepository.findAll().stream()
                .map(job -> new ScoredJob(job, calculateMatchScore(job, request)))
                .filter(scoredJob -> !hasActiveCriteria(request) || scoredJob.score().comparedFields() == 0
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
                workMode,
                job.getExperienceLevel() == null ? "Unknown" : job.getExperienceLevel().getName(),
                job.getMinSalary(),
                job.getMaxSalary(),
                job.getDatePosted(),
                job.getSourceWebsite(),
                matchScore,
                toleranceLevel(matchScore),
                skills
        );
    }

    private MatchScore calculateMatchScore(Job job, JobFilterRequest request) {
        MatchScore score = new MatchScore();
        JobFilterRequest.JobCriteria jobCriteria = request.job();
        JobFilterRequest.LocationCriteria locationCriteria = request.location();

        if (jobCriteria != null) {
            score.addText(job.getCompanyName(), jobCriteria.companyname());
            score.addText(job.getDescription(), jobCriteria.description());
            score.addText(job.getSourceWebsite(), jobCriteria.sourceWebsite());
            score.addText(job.getExperienceLevel() == null ? null : job.getExperienceLevel().getName(),
                    jobCriteria.experienceLevelName());
            score.addExact(job.getDatePosted(), jobCriteria.datePosted());
            score.addSalaryRange(job.getMinSalary(), job.getMaxSalary(), jobCriteria.minSalary(), jobCriteria.maxSalary());
            score.addSalaryRange(job.getPredictedMinSalary(), job.getPredictedMaxSalary(),
                    jobCriteria.predictedMinSalary(), jobCriteria.predictedMaxSalary());
            score.addExperience(job.getRequiredExperience(), jobCriteria.requiredExperience());
        }

        if (locationCriteria != null && !isRemote(job)) {
            Location location = job.getLocation();
            score.addText(location == null ? null : location.getCityDistrict(), locationCriteria.cityDistrict());
            score.addText(location == null ? null : location.getCity(), locationCriteria.city());
            score.addText(location == null ? null : location.getRegion(), locationCriteria.region());
            score.addText(location == null ? null : location.getCountry(), locationCriteria.country());
            score.addExact(location == null ? null : location.getLatitude(), locationCriteria.latitude());
            score.addExact(location == null ? null : location.getLongitude(), locationCriteria.longitude());
        }

        addSkillScore(score, job, request.skills());
        addWorkTypeScore(score, job, request.workTypes());

        return score;
    }

    private void addSkillScore(MatchScore score, Job job, List<String> requestedSkills) {
        if (isEmpty(requestedSkills)) {
            return;
        }

        List<String> jobSkills = jobSkillRepository.findByJob_Id(job.getId()).stream()
                .flatMap(jobSkill -> List.of(jobSkill.getSkill().getId(), jobSkill.getSkill().getName()).stream())
                .map(this::normalize)
                .toList();

        requestedSkills.stream()
                .filter(this::hasText)
                .map(this::normalize)
                .forEach(requestedSkill -> score.add(jobSkills.contains(requestedSkill)));
    }

    private void addWorkTypeScore(MatchScore score, Job job, List<String> requestedWorkTypes) {
        if (isEmpty(requestedWorkTypes)) {
            return;
        }

        List<String> jobWorkTypes = jobWorkTypes(job);

        requestedWorkTypes.stream()
                .filter(this::hasText)
                .map(this::normalize)
                .forEach(requestedWorkType -> score.add(jobWorkTypes.contains(requestedWorkType)));
    }

    private boolean hasActiveCriteria(JobFilterRequest request) {
        return hasActiveJobCriteria(request.job())
                || hasActiveLocationCriteria(request.location())
                || !isEmpty(request.skills())
                || !isEmpty(request.workTypes());
    }

    private boolean hasActiveJobCriteria(JobFilterRequest.JobCriteria criteria) {
        return criteria != null && (hasText(criteria.companyname())
                || hasText(criteria.description())
                || hasText(criteria.sourceWebsite())
                || hasText(criteria.experienceLevelName())
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
        return value.toLowerCase().trim();
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
        private int matchedFields;
        private int comparedFields;

        void add(boolean matched) {
            comparedFields++;
            if (matched) {
                matchedFields++;
            }
        }

        void addText(String actual, String expected) {
            if (!hasText(expected) || !hasText(actual)) {
                return;
            }
            add(normalize(actual).contains(normalize(expected)));
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

        void addExperience(Integer requiredExperience, Integer userExperience) {
            if (requiredExperience == null || userExperience == null) {
                return;
            }

            int requiredMonths = toMonths(requiredExperience);
            int userMonths = toMonths(userExperience);
            add(userMonths >= Math.ceil(requiredMonths * EXPERIENCE_TOLERANCE));
        }

        int matchedFields() {
            return matchedFields;
        }

        int comparedFields() {
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
