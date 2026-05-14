package si.um.feri.smartjobs.skillType.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import si.um.feri.smartjobs.skillType.entity.SkillType;

import java.util.Optional;

public interface SkillTypeRepository extends JpaRepository<SkillType, String> {
    Optional<SkillType> findByNameIgnoreCase(String name);
}