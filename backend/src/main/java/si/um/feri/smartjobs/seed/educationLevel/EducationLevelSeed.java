package si.um.feri.smartjobs.seed.educationLevel;

import org.springframework.stereotype.Component;
import si.um.feri.smartjobs.educationLevel.entity.EducationLevel;
import si.um.feri.smartjobs.educationLevel.repository.EducationLevelRepository;

import java.util.List;

@Component
public class EducationLevelSeed {

    private final EducationLevelRepository repository;

    public EducationLevelSeed(EducationLevelRepository repository) {
        this.repository = repository;
    }

    public void seed() {
        if (repository.count() > 0) return;

        repository.saveAll(List.of(
                new EducationLevel("edu-primary", "Primary"),
                new EducationLevel("edu-lower-vocational", "Lower vocational"),
                new EducationLevel("edu-secondary-vocational", "Secondary vocational"),
                new EducationLevel("edu-secondary-general", "Secondary general"),
                new EducationLevel("edu-higher-vocational", "Higher vocational"),
                new EducationLevel("edu-bachelor", "Bachelor"),
                new EducationLevel("edu-master", "Master"),
                new EducationLevel("edu-phd", "PhD"),
                new EducationLevel("edu-certification", "Professional certification"),
                new EducationLevel("edu-not-specified", "Not specified")
        ));
    }
}