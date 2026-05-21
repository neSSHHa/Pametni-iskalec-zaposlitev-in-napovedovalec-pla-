package si.um.feri.smartjobs.cv.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import si.um.feri.smartjobs.cv.dto.CvJobMatchResponse;
import si.um.feri.smartjobs.cv.dto.CvTextExtractionResponse;
import si.um.feri.smartjobs.cv.service.CvJobMatchingService;
import si.um.feri.smartjobs.cv.service.CvTextExtractionService;
import si.um.feri.smartjobs.ai.dto.AiJobFilterDebugResponse;

@RestController
@RequestMapping("/api/cv")
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
    public CvTextExtractionResponse extractText(@RequestPart("file") MultipartFile file) {
        String text = cvTextExtractionService.extractText(file);

        return new CvTextExtractionResponse(
                file.getOriginalFilename(),
                file.getContentType(),
                text
        );
    }

    @PostMapping("/jobs/filter")
    public CvJobMatchResponse filterJobsFromCv(@RequestPart("file") MultipartFile file) {
        return cvJobMatchingService.matchJobs(file);
    }

//za direktno response samo na suitable jobs zameni toa pogore so ova
//@PostMapping("/jobs/filter")
//public List<JobDto> filterJobsFromCv(@RequestPart("file") MultipartFile file) {
  //  return cvJobMatchingService.matchJobs(file).jobs();
//}

    @PostMapping("/extract-filter")
    public AiJobFilterDebugResponse extractFilterFromCv(@RequestPart("file") MultipartFile file) {
        return cvJobMatchingService.extractFilterDebug(file);
    }

//ova e za testing na cv to query like language
    @PostMapping("/rewrite-profile")
    public String rewriteCvToProfileText(@RequestPart("file") MultipartFile file) {
        String extractedText = cvTextExtractionService.extractText(file);
        return cvJobMatchingService.rewriteCvToProfileText(extractedText);
    }
}
