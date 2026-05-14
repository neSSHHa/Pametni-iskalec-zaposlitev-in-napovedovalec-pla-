package si.um.feri.smartjobs.educationLevel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import si.um.feri.smartjobs.educationLevel.entity.EducationLevel;

import java.util.Optional;

public interface EducationLevelRepository extends JpaRepository<EducationLevel, String> {
    Optional<EducationLevel> findByNameIgnoreCase(String name);
}