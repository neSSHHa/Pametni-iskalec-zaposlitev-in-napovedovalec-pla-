package si.um.feri.smartjobs.cv.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import si.um.feri.smartjobs.cv.dto.CvJobMatchResponse;
import si.um.feri.smartjobs.cv.dto.CvTextExtractionResponse;
import si.um.feri.smartjobs.cv.service.CvJobMatchingService;
import si.um.feri.smartjobs.cv.service.CvTextExtractionService;
import si.um.feri.smartjobs.ai.dto.AiJobFilterDebugResponse;

import java.util.Map;

@RestController
@RequestMapping("/api/cv")
@Tag(name = "CV", description = "Upload CV files, extract text, derive job filters, and match CV profiles to relevant jobs.")
public class CvController {

    private final CvTextExtractionService cvTextExtractionService;
    private final CvJobMatchingService cvJobMatchingService;

    public CvController(
            CvTextExtractionService cvTextExtractionService,
            CvJobMatchingService cvJobMatchingService
    ) {
        this.cvTextExtractionService = cvTextExtractionService;
        this.cvJobMatchingService = cvJobMatchingService;
    }

    @PostMapping("/extract-text")
    @Operation(
            summary = "Extract text from a CV file",
            description = "Accepts a PDF, DOC, DOCX, or text-based CV file and returns the extracted text content.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "multipart/form-data")
            )
    )
    @ApiResponse(responseCode = "200", description = "CV text was extracted successfully.")
    public CvTextExtractionResponse extractText(@RequestPart("file") MultipartFile file) {
        String text = cvTextExtractionService.extractText(file);

        return new CvTextExtractionResponse(
                file.getOriginalFilename(),
                file.getContentType(),
                text
        );
    }

    @PostMapping("/jobs/filter")
    @Operation(
            summary = "Match jobs from an uploaded CV",
            description = "Extracts CV text, converts the profile into search criteria, and returns matching job postings. The mode parameter supports 'fast' and 'thinking'.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "multipart/form-data")
            )
    )
    @ApiResponse(responseCode = "200", description = "The CV was processed and matching jobs were returned.")
    public CvJobMatchResponse filterJobsFromCv(
            @Parameter(description = "CV file to analyze.", schema = @Schema(type = "string", format = "binary"))
            @RequestPart("file") MultipartFile file,
            @Parameter(in = ParameterIn.QUERY, description = "Extraction mode. Use 'fast' for local extraction or 'thinking' for AI-assisted extraction.", example = "fast")
            @RequestParam(defaultValue = "fast") String mode
    ) {
        return cvJobMatchingService.matchJobs(file, mode);
    }

//za direktno response samo na suitable jobs zameni toa pogore so ova
//@PostMapping("/jobs/filter")
//public List<JobDto> filterJobsFromCv(@RequestPart("file") MultipartFile file) {
  //  return cvJobMatchingService.matchJobs(file).jobs();
//}

    @PostMapping("/extract-filter")
    @Operation(
            summary = "Debug filter extraction from CV",
            description = "Extracts a filter request from a CV and returns debug information for the generated search criteria.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "multipart/form-data")
            )
    )
    @ApiResponse(responseCode = "200", description = "The CV filter extraction debug payload was returned successfully.")
    public AiJobFilterDebugResponse extractFilterFromCv(@RequestPart("file") MultipartFile file) {
        return cvJobMatchingService.extractFilterDebug(file);
    }

//ova e za testing na cv to query like language
    @PostMapping("/rewrite-profile")
    @Operation(
            summary = "Rewrite CV as a search profile",
            description = "Extracts CV text and rewrites it into a concise profile text that can be used for job search and matching."
    )
    @ApiResponse(responseCode = "200", description = "The CV was rewritten into profile text.")
    public String rewriteCvToProfileText(@RequestPart("file") MultipartFile file) {
        String extractedText = cvTextExtractionService.extractText(file);
        return cvJobMatchingService.rewriteCvToProfileText(extractedText);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCv(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", exception.getMessage()));
    }
}
