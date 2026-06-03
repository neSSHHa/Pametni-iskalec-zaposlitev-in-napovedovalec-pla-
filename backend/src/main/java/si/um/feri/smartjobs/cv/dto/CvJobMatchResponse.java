package si.um.feri.smartjobs.cv.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import si.um.feri.smartjobs.job.dto.JobDto;
import si.um.feri.smartjobs.job.dto.JobFilterRequest;

import java.util.List;

@Schema(description = "Job matching response generated from an uploaded CV.")
public record CvJobMatchResponse(
        @Schema(description = "Original uploaded filename.", example = "candidate-cv.pdf")
        String filename,
        @Schema(description = "Detected file content type.", example = "application/pdf")
        String contentType,
        @Schema(description = "Text extracted from the uploaded CV.")
        String extractedText,
        @Schema(description = "Filter criteria derived from the CV.")
        JobFilterRequest filterRequest,
        @Schema(description = "Jobs matching the extracted CV profile.")
        List<JobDto> jobs,
        @Schema(description = "Total number of matching jobs.", example = "42")
        long totalCount,
        @Schema(description = "Current page index.", example = "0")
        int page,
        @Schema(description = "Current page size.", example = "50")
        int size,
        @Schema(description = "Whether more matching jobs are available.", example = "false")
        boolean hasMore,
        @Schema(description = "Average match score for returned jobs.", example = "84")
        Integer averageMatch
) {
}
