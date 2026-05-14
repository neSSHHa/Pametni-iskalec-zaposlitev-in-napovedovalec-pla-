package si.um.feri.smartjobs.skillRelation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import si.um.feri.smartjobs.skillRelation.entity.SkillRelation;

import java.util.List;

public interface SkillRelationRepository extends JpaRepository<SkillRelation, String> {

    List<SkillRelation> findBySourceSkill_Id(String sourceSkillId);

    List<SkillRelation> findByTargetSkill_Id(String targetSkillId);
}