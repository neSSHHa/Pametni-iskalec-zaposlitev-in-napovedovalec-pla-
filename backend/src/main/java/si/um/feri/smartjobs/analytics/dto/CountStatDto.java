package si.um.feri.smartjobs.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Generic count and percentage statistic used by analytics endpoints.")
public record CountStatDto(
        @Schema(description = "Statistic label.", example = "Java")
        String label,
        @Schema(description = "Number of matching jobs.", example = "320")
        long count,
        @Schema(description = "Percentage share in the selected category.", example = "18.4")
        double percentage
) {
}
