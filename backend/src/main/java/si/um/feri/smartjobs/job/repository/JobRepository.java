package si.um.feri.smartjobs.job.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import si.um.feri.smartjobs.job.entity.Job;

import java.util.List;
import java.util.Collection;

public interface JobRepository extends JpaRepository<Job, String> {
    List<Job> findByIdIn(Collection<String> ids);

    List<Job> findByJobNameContainingIgnoreCase(String jobName);

    List<Job> findByJobNameContainingIgnoreCaseOrCompanyNameContainingIgnoreCase(String jobName, String companyName);
    
    List<Job> findByLocation_Id(String locationId);

    List<Job> findByExperienceLevel_Id(String experienceLevelId);

    List<Job> findByExperienceLevel_NameIgnoreCase(String experienceLevelName);

    List<Job> findByEducationLevel_NameIgnoreCase(String educationLevelName);

    List<Job> findByLocation_CityContainingIgnoreCase(String city);

    List<Job> findByLocation_RegionContainingIgnoreCase(String region);

    List<Job> findByLocation_CountryContainingIgnoreCase(String country);
}
