package si.um.feri.smartjobs.job.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import si.um.feri.smartjobs.analytics.dto.AnalyticsDashboardDto;

import java.util.List;

@Schema(description = "Paginated job search response.")
public record JobSearchResponse(
        @Schema(description = "Jobs returned for the current page.")
        List<JobDto> jobs,
        @Schema(description = "Total number of jobs matching the query.", example = "128")
        long totalCount,
        @Schema(description = "Current zero-based page index.", example = "0")
        int page,
        @Schema(description = "Requested page size.", example = "50")
        int size,
        @Schema(description = "Whether more jobs are available after the current page.", example = "true")
        boolean hasMore,
        @Schema(description = "Average match score for the returned jobs.", example = "82")
        Integer averageMatch,
        @Schema(description = "Filter request used to produce this response, when applicable.")
        JobFilterRequest filterRequest,
        @Schema(description = "Analytics calculated over all jobs matching the query, not only the returned page.")
        AnalyticsDashboardDto analytics
) {
        public JobSearchResponse(
                List<JobDto> jobs,
                long totalCount,
                int page,
                int size,
                boolean hasMore,
                Integer averageMatch,
                JobFilterRequest filterRequest
        ) {
                this(jobs, totalCount, page, size, hasMore, averageMatch, filterRequest, null);
        }
}
