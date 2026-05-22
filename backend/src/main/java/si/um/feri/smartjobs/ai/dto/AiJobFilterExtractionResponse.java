package si.um.feri.smartjobs.ai.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/*
Trenutno dela:
text -> mock AI response -> JobFilterRequest

Kasneje bomo samo zamenjali:
mockAiExtraction(text)
z realnim:
aiServiceClient.extractFilter(...)
Vsa ostala logika ostane.
*/

public record AiJobFilterExtractionResponse(
        JobData job,
        LocationData location,
        List<String> workTypes,
        List<String> skills,
        List<String> unknownSkills
) {
    public record JobData(
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

    public record LocationData(
        String cityDistrict,
        String city,
        List<String> cities,
        String region,
        List<String> regions,
        String country,
        List<String> countries,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
}
