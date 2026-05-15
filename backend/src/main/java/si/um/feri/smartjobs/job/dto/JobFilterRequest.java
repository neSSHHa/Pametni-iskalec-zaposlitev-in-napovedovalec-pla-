package si.um.feri.smartjobs.job.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record JobFilterRequest(
        JobCriteria job,
        LocationCriteria location,
        List<String> workTypes,
        List<String> skills
) {
    public record JobCriteria(
            String companyname,
            String jobname,
            String description,
            Integer requiredExperience,
            BigDecimal predictedMinSalary,
            BigDecimal predictedMaxSalary,
            String sourceWebsite,
            LocalDate datePosted,
            BigDecimal minSalary,
            BigDecimal maxSalary,
            String experienceLevelName,
            String educationLevel
    ) {
    }

    public record LocationCriteria(
            String cityDistrict,
            String city,
            String region,
            String country,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
    }
}
