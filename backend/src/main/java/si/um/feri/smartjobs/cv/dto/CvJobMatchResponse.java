package si.um.feri.smartjobs.cv.dto;

import si.um.feri.smartjobs.job.dto.JobDto;
import si.um.feri.smartjobs.job.dto.JobFilterRequest;

import java.util.List;

public record CvJobMatchResponse(
        String filename,
        String contentType,
        String extractedText,
        JobFilterRequest filterRequest,
        List<JobDto> jobs,
        long totalCount,
        int page,
        int size,
        boolean hasMore,
        Integer averageMatch
) {
}
