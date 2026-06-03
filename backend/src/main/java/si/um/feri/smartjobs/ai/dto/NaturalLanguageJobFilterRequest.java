package si.um.feri.smartjobs.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Natural language prompt used for AI-assisted job filtering.")
public record NaturalLanguageJobFilterRequest(
        @Schema(description = "User-written prompt describing the desired job search.", example = "Find remote Java backend jobs in Ljubljana with at least two years of experience.")
        String text,
        @Schema(description = "Extraction mode. Use fast for local extraction or thinking for AI-assisted extraction.", example = "fast", allowableValues = {"fast", "thinking"})
        String mode
) {
}
