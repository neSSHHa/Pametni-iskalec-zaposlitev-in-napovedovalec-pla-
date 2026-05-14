package si.um.feri.smartjobs.skill.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import si.um.feri.smartjobs.skill.entity.Skill;

import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, String> {

    Optional<Skill> findByNameIgnoreCase(String name);

    List<Skill> findByNameContainingIgnoreCase(String name);

    List<Skill> findBySkillType_Id(String skillTypeId);
}