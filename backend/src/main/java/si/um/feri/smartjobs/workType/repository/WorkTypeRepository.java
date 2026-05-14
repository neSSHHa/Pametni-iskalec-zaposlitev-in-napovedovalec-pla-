package si.um.feri.smartjobs.workType.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import si.um.feri.smartjobs.workType.entity.WorkType;

import java.util.Optional;

public interface WorkTypeRepository extends JpaRepository<WorkType, String> {
    Optional<WorkType> findByNameIgnoreCase(String name);
}