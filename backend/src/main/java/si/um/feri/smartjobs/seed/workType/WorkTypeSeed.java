package si.um.feri.smartjobs.seed.workType;

import org.springframework.stereotype.Component;
import si.um.feri.smartjobs.workType.entity.WorkType;
import si.um.feri.smartjobs.workType.repository.WorkTypeRepository;

import java.util.List;

@Component
public class WorkTypeSeed {

    private final WorkTypeRepository repository;

    public WorkTypeSeed(WorkTypeRepository repository) {
        this.repository = repository;
    }

    public void seed() {
        if (repository.count() > 0) return;

        repository.saveAll(List.of(
                new WorkType("wt-onsite", "On-site"),
                new WorkType("wt-remote", "Remote"),
                new WorkType("wt-hybrid", "Hybrid"),
                new WorkType("wt-field", "Field work"),
                new WorkType("wt-shift", "Shift work"),
                new WorkType("wt-student", "Student work"),
                new WorkType("wt-full-time", "Full-time"),
                new WorkType("wt-part-time", "Part-time"),
                new WorkType("wt-temporary", "Temporary"),
                new WorkType("wt-not-specified", "Not specified")
        ));
    }
}