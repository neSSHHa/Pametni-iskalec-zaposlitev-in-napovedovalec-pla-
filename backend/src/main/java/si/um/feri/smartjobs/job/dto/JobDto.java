package si.um.feri.smartjobs.job.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record JobDto(
        String id,
        String title,
        String companyName,
        String description,
        String location,
        String city,
        String region,
        String country,
        BigDecimal latitude,
        BigDecimal longitude,
        String workMode,
        String experienceLevel,
        String educationLevel,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        LocalDate postedDate,
        String sourceUrl,
        int matchScore,
        String matchLevel,
        List<String> skills
) {
}
