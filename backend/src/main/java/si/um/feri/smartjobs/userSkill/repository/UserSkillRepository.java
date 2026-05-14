package si.um.feri.smartjobs.userSkill.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import si.um.feri.smartjobs.userSkill.entity.UserSkill;

import java.util.List;

public interface UserSkillRepository extends JpaRepository<UserSkill, String> {
    List<UserSkill> findByUser_Id(String userId);
    List<UserSkill> findBySkill_Id(String skillId);
}