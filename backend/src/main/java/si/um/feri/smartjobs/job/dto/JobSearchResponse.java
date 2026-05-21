package si.um.feri.smartjobs.job.dto;

import java.util.List;

public record JobSearchResponse(
        List<JobDto> jobs,
        long totalCount,
        int page,
        int size,
        boolean hasMore,
        Integer averageMatch,
        JobFilterRequest filterRequest
) {
}
