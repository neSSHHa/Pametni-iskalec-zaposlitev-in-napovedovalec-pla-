package si.um.feri.smartjobs.workTypeJob.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import si.um.feri.smartjobs.workTypeJob.entity.WorkTypeJob;

import java.util.List;

public interface WorkTypeJobRepository extends JpaRepository<WorkTypeJob, String> {

    List<WorkTypeJob> findByJob_Id(String jobId);

    List<WorkTypeJob> findByJob_IdIn(List<String> jobIds);

    List<WorkTypeJob> findByWorkType_Id(String workTypeId);
}
