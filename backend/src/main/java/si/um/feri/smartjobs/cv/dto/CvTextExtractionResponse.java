package si.um.feri.smartjobs.cv.dto;

public record CvTextExtractionResponse(
        String filename,
        String contentType,
        String text
) {
}
