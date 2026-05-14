package si.um.feri.smartjobs.experienceLevel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import si.um.feri.smartjobs.experienceLevel.entity.ExperienceLevel;

import java.util.Optional;

public interface ExperienceLevelRepository extends JpaRepository<ExperienceLevel, String> {
    Optional<ExperienceLevel> findByNameIgnoreCase(String name);
}