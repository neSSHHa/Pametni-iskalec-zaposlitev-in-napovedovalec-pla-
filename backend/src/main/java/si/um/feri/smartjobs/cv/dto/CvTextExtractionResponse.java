package si.um.feri.smartjobs.cv.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Text extracted from an uploaded CV file.")
public record CvTextExtractionResponse(
        @Schema(description = "Original uploaded filename.", example = "nenad-cv.pdf")
        String filename,
        @Schema(description = "Detected file content type.", example = "application/pdf")
        String contentType,
        @Schema(description = "Text extracted from the uploaded CV.")
        String text
) {
}
