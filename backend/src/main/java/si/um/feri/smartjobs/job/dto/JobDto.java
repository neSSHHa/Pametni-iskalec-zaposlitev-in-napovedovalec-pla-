package si.um.feri.smartjobs.job.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Job posting returned by search, filtering, CV matching, and AI-assisted search endpoints.")
public record JobDto(
        @Schema(description = "Unique job identifier.", example = "job-10042")
        String id,
        @Schema(description = "Job title.", example = "Senior Java Developer")
        String title,
        @Schema(description = "Company offering the job.", example = "SmartJobs d.o.o.")
        String companyName,
        @Schema(description = "Original or normalized job description.")
        String description,
        @Schema(description = "Human-readable location label.", example = "Ljubljana, Slovenia")
        String location,
        @Schema(description = "City where the job is located.", example = "Ljubljana")
        String city,
        @Schema(description = "Region where the job is located.", example = "Osrednjeslovenska")
        String region,
        @Schema(description = "Country where the job is located.", example = "Slovenia")
        String country,
        @Schema(description = "Latitude coordinate if known.", example = "46.056946")
        BigDecimal latitude,
        @Schema(description = "Longitude coordinate if known.", example = "14.505751")
        BigDecimal longitude,
        @Schema(description = "Work mode or work type.", example = "Remote")
        String workMode,
        @Schema(description = "Required or inferred experience level.", example = "Senior")
        String experienceLevel,
        @Schema(description = "Required or preferred education level.", example = "Bachelor")
        String educationLevel,
        @Schema(description = "Minimum advertised or predicted salary.", example = "3000")
        BigDecimal salaryMin,
        @Schema(description = "Maximum advertised or predicted salary.", example = "4500")
        BigDecimal salaryMax,
        @Schema(description = "Date when the job was posted.", example = "2026-06-01")
        LocalDate postedDate,
        @Schema(description = "Original job source URL.")
        String sourceUrl,
        @Schema(description = "Matching score calculated for the current query or profile.", example = "87")
        int matchScore,
        @Schema(description = "Confidence score for matching or extracted criteria.", example = "78")
        int confidenceScore,
        @Schema(description = "Readable match level for UI display.", example = "High")
        String matchLevel,
        @Schema(description = "Skills associated with the job.", example = "[\"Java\", \"Spring Boot\", \"MySQL\"]")
        List<String> skills
) {
}
