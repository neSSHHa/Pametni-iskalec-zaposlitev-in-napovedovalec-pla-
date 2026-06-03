package si.um.feri.smartjobs.job.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Structured filter request for job search and salary prediction.")
public record JobFilterRequest(
        @Schema(description = "Job-related criteria such as company, title, salary, experience, education, and source.")
        JobCriteria job,
        @Schema(description = "Location criteria such as city, region, country, and coordinates.")
        LocationCriteria location,
        @Schema(description = "Accepted work types or work modes.", example = "[\"Remote\", \"Hybrid\"]")
        List<String> workTypes,
        @Schema(description = "Required or preferred skills.", example = "[\"Java\", \"Spring Boot\", \"SQL\"]")
        List<String> skills
) {
    @Schema(description = "Job-specific filter criteria.")
    public record JobCriteria(
            @Schema(description = "Company name.", example = "SmartJobs d.o.o.")
            String companyname,
            @Schema(description = "Job title or role.", example = "Java Developer")
            String jobname,
            @Schema(description = "Keywords expected in the job description.", example = "backend APIs microservices")
            String description,
            @Schema(description = "Required years of experience.", example = "2")
            Integer requiredExperience,
            @Schema(description = "Predicted minimum salary filter.", example = "2500")
            BigDecimal predictedMinSalary,
            @Schema(description = "Predicted maximum salary filter.", example = "4500")
            BigDecimal predictedMaxSalary,
            @Schema(description = "Source website name.", example = "CareerJet")
            String sourceWebsite,
            @Schema(description = "Date when the job was posted.", example = "2026-06-01")
            LocalDate datePosted,
            @Schema(description = "Minimum salary filter.", example = "2500")
            BigDecimal minSalary,
            @Schema(description = "Maximum salary filter.", example = "4500")
            BigDecimal maxSalary,
            @Schema(description = "Experience level name.", example = "Mid")
            String experienceLevelName,
            @Schema(description = "Education level name.", example = "Bachelor")
            String educationLevel
    ) {
    }

    @Schema(description = "Location-specific filter criteria.")
    public record LocationCriteria(
            @Schema(description = "City district or neighborhood.", example = "Center")
            String cityDistrict,
            @Schema(description = "Single city filter.", example = "Ljubljana")
            String city,
            @Schema(description = "Multiple accepted cities.", example = "[\"Ljubljana\", \"Maribor\"]")
            List<String> cities,
            @Schema(description = "Single region filter.", example = "Osrednjeslovenska")
            String region,
            @Schema(description = "Multiple accepted regions.", example = "[\"Osrednjeslovenska\", \"Podravska\"]")
            List<String> regions,
            @Schema(description = "Single country filter.", example = "Slovenia")
            String country,
            @Schema(description = "Multiple accepted countries.", example = "[\"Slovenia\", \"Austria\"]")
            List<String> countries,
            @Schema(description = "Latitude coordinate for location-based filtering.", example = "46.056946")
            BigDecimal latitude,
            @Schema(description = "Longitude coordinate for location-based filtering.", example = "14.505751")
            BigDecimal longitude
    ) {
    }
}
