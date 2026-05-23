package si.um.feri.smartjobs.jobSkill.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import si.um.feri.smartjobs.jobSkill.entity.JobSkill;

import java.util.Collection;
import java.util.List;

public interface JobSkillRepository extends JpaRepository<JobSkill, String> {

    List<JobSkill> findByJob_Id(String jobId);

    List<JobSkill> findByJob_IdIn(List<String> jobIds);

    List<JobSkill> findBySkill_Id(String skillId);

    List<JobSkill> findBySkill_IdIn(Collection<String> skillIds);

    List<JobSkill> findBySkill_NameIn(Collection<String> skillNames);
}
