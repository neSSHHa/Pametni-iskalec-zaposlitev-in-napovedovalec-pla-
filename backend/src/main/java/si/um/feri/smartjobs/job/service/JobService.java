package si.um.feri.smartjobs.job.service;

import org.springframework.stereotype.Service;
import si.um.feri.smartjobs.job.dto.JobDto;
import si.um.feri.smartjobs.job.entity.Job;
import si.um.feri.smartjobs.job.repository.JobRepository;

import java.util.List;

@Service
public class JobService {
    private final JobRepository jobRepository;

    public JobService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
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

    private JobDto toDto(Job job) {
        return new JobDto(
                job.getId(),
                job.getJobName(),
                job.getCompanyName(),
                job.getDescription(),
                "Slovenia",
                "On-site/Hybrid",
                job.getRequiredExperience() == null ? "Unknown" : job.getRequiredExperience() + "+ years",
                job.getMinSalary(),
                job.getMaxSalary(),
                job.getDatePosted(),
                job.getSourceWebsite(),
                90,
                List.of()
        );
    }
}
