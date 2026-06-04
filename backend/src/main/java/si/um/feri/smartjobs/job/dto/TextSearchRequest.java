package si.um.feri.smartjobs.job.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Free-text job search request.")
public record TextSearchRequest(
        @Schema(description = "Search text entered by the user.", example = "Java developer remote Ljubljana")
        String query
) {
}
