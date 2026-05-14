package si.um.feri.smartjobs.seed.experienceLevel;

import org.springframework.stereotype.Component;
import si.um.feri.smartjobs.experienceLevel.entity.ExperienceLevel;
import si.um.feri.smartjobs.experienceLevel.repository.ExperienceLevelRepository;

import java.util.List;

@Component
public class ExperienceLevelSeed {

    private final ExperienceLevelRepository repository;

    public ExperienceLevelSeed(ExperienceLevelRepository repository) {
        this.repository = repository;
    }

    public void seed() {
        if (repository.count() > 0) return;

        repository.saveAll(List.of(
                new ExperienceLevel("exp-intern", "Intern"),
                new ExperienceLevel("exp-entry", "Entry"),
                new ExperienceLevel("exp-junior", "Junior"),
                new ExperienceLevel("exp-mid", "Mid"),
                new ExperienceLevel("exp-senior", "Senior"),
                new ExperienceLevel("exp-lead", "Lead"),
                new ExperienceLevel("exp-manager", "Manager"),
                new ExperienceLevel("exp-not-specified", "Not specified")
        ));
    }
}