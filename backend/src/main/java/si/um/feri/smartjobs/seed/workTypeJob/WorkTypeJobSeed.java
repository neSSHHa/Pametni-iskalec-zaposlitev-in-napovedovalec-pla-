package si.um.feri.smartjobs.seed.workTypeJob;

import org.springframework.stereotype.Component;
import si.um.feri.smartjobs.job.repository.JobRepository;
import si.um.feri.smartjobs.workType.repository.WorkTypeRepository;
import si.um.feri.smartjobs.workTypeJob.entity.WorkTypeJob;
import si.um.feri.smartjobs.workTypeJob.repository.WorkTypeJobRepository;

import java.util.ArrayList;
import java.util.List;

@Component
public class WorkTypeJobSeed {

    private final WorkTypeJobRepository repository;
    private final JobRepository jobRepository;
    private final WorkTypeRepository workTypeRepository;

    public WorkTypeJobSeed(WorkTypeJobRepository repository,
                           JobRepository jobRepository,
                           WorkTypeRepository workTypeRepository) {
        this.repository = repository;
        this.jobRepository = jobRepository;
        this.workTypeRepository = workTypeRepository;
    }

    public void seed() {
        if (repository.count() > 0) return;

        List<WorkTypeJob> items = new ArrayList<>();

        add(items, "job-001", "wt-hybrid", "wt-full-time");
        add(items, "job-002", "wt-hybrid", "wt-full-time");
        add(items, "job-003", "wt-remote", "wt-full-time");
        add(items, "job-004", "wt-hybrid", "wt-remote");
        add(items, "job-005", "wt-hybrid", "wt-full-time");
        add(items, "job-006", "wt-remote", "wt-full-time");
        add(items, "job-007", "wt-hybrid", "wt-full-time");
        add(items, "job-008", "wt-hybrid", "wt-full-time");
        add(items, "job-009", "wt-student", "wt-part-time");
        add(items, "job-010", "wt-hybrid", "wt-full-time");
        add(items, "job-011", "wt-remote", "wt-full-time");
        add(items, "job-012", "wt-hybrid", "wt-full-time");

        add(items, "job-013", "wt-onsite", "wt-shift");
        add(items, "job-014", "wt-onsite", "wt-shift");
        add(items, "job-015", "wt-onsite", "wt-full-time");
        add(items, "job-016", "wt-onsite", "wt-shift");
        add(items, "job-017", "wt-field", "wt-full-time");
        add(items, "job-018", "wt-onsite", "wt-full-time");

        add(items, "job-019", "wt-onsite", "wt-full-time");
        add(items, "job-020", "wt-onsite", "wt-full-time");
        add(items, "job-021", "wt-onsite", "wt-full-time");
        add(items, "job-022", "wt-onsite", "wt-full-time");

        add(items, "job-023", "wt-hybrid", "wt-full-time");
        add(items, "job-024", "wt-onsite", "wt-full-time");
        add(items, "job-025", "wt-onsite", "wt-full-time");
        add(items, "job-026", "wt-hybrid", "wt-full-time");
        add(items, "job-027", "wt-onsite", "wt-full-time");

        add(items, "job-028", "wt-onsite", "wt-full-time");
        add(items, "job-029", "wt-hybrid", "wt-full-time");
        add(items, "job-030", "wt-onsite", "wt-full-time");

        add(items, "job-031", "wt-onsite", "wt-full-time");
        add(items, "job-032", "wt-onsite", "wt-shift");
        add(items, "job-033", "wt-onsite", "wt-full-time");
        add(items, "job-034", "wt-onsite", "wt-full-time");
        add(items, "job-035", "wt-onsite", "wt-full-time");
        add(items, "job-036", "wt-onsite", "wt-full-time");
        add(items, "job-037", "wt-onsite", "wt-shift");

        add(items, "job-038", "wt-onsite", "wt-full-time");
        add(items, "job-039", "wt-onsite", "wt-part-time");
        add(items, "job-040", "wt-onsite", "wt-full-time");
        add(items, "job-041", "wt-field", "wt-full-time");
        add(items, "job-042", "wt-onsite", "wt-full-time");

        add(items, "job-043", "wt-onsite", "wt-shift");
        add(items, "job-044", "wt-onsite", "wt-shift");
        add(items, "job-045", "wt-field", "wt-part-time");

        add(items, "job-046", "wt-onsite", "wt-full-time");
        add(items, "job-047", "wt-field", "wt-full-time");

        add(items, "job-048", "wt-onsite", "wt-full-time");
        add(items, "job-049", "wt-onsite", "wt-full-time");

        add(items, "job-050", "wt-onsite", "wt-full-time");

        add(items, "job-051", "wt-hybrid", "wt-full-time");
        add(items, "job-052", "wt-hybrid", "wt-full-time");
        add(items, "job-053", "wt-hybrid", "wt-full-time");
        add(items, "job-054", "wt-hybrid", "wt-full-time");
        add(items, "job-055", "wt-hybrid", "wt-full-time");
        add(items, "job-056", "wt-remote", "wt-full-time");
        add(items, "job-057", "wt-hybrid", "wt-full-time");
        add(items, "job-058", "wt-hybrid", "wt-full-time");
        add(items, "job-059", "wt-hybrid", "wt-full-time");
        add(items, "job-060", "wt-remote", "wt-full-time");
        add(items, "job-061", "wt-hybrid", "wt-full-time");
        add(items, "job-062", "wt-hybrid", "wt-full-time");
        add(items, "job-063", "wt-onsite", "wt-full-time");
        add(items, "job-064", "wt-onsite", "wt-full-time");
        add(items, "job-065", "wt-hybrid", "wt-full-time");
        add(items, "job-066", "wt-remote", "wt-full-time");
        add(items, "job-067", "wt-hybrid", "wt-full-time");
        add(items, "job-068", "wt-onsite", "wt-full-time");
        add(items, "job-069", "wt-remote", "wt-full-time");
        add(items, "job-070", "wt-hybrid", "wt-full-time");
        add(items, "job-071", "wt-hybrid", "wt-full-time");
        add(items, "job-072", "wt-hybrid", "wt-full-time");
        add(items, "job-073", "wt-hybrid", "wt-full-time");
        add(items, "job-074", "wt-hybrid", "wt-full-time");
        add(items, "job-075", "wt-hybrid", "wt-full-time");

        repository.saveAll(items);
    }

    private int counter = 1;

private void add(List<WorkTypeJob> items, String jobId, String... workTypeIds) {
    for (String workTypeId : workTypeIds) {
        String id = "wtj-" + counter++;
        items.add(new WorkTypeJob(
                id,
                jobRepository.findById(jobId).orElseThrow(),
                workTypeRepository.findById(workTypeId).orElseThrow()
        ));
    }
}
}
