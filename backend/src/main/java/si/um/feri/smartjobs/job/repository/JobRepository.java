package si.um.feri.smartjobs.job.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import si.um.feri.smartjobs.job.entity.Job;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, String> {
    List<Job> findByJobNameContainingIgnoreCaseOrCompanyNameContainingIgnoreCase(String jobName, String companyName);
    
        List<Job> findByLocation_Id(String locationId);

    List<Job> findByExperienceLevel_Id(String experienceLevelId);

    List<Job> findByLocation_CityContainingIgnoreCase(String city);
}
