package si.um.feri.smartjobs.ai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import si.um.feri.smartjobs.ai.client.AiServiceClient;
import si.um.feri.smartjobs.ai.dto.AiExtractionRequest;
import si.um.feri.smartjobs.ai.dto.AiJobFilterExtractionResponse;
import si.um.feri.smartjobs.ai.dto.CvRewriteRequest;
import si.um.feri.smartjobs.ai.dto.CvRewriteResponse;

@RestController
@RequestMapping("/api/ai")
public class AiServiceController {

    private final AiServiceClient aiServiceClient;

    public AiServiceController(AiServiceClient aiServiceClient) {
        this.aiServiceClient = aiServiceClient;
    }

    @GetMapping("/health")
    public String health() {
        return "ok";
    }

    @PostMapping("/jobs/extract")
    public AiJobFilterExtractionResponse extractJobFilter(@RequestBody AiExtractionRequest request) {
        return aiServiceClient.extractJobFilter(
                request.text(),
                request.allowedSkills(),
                request.allowedEducationLevels(),
                request.allowedExperienceLevels(),
                request.allowedWorkTypes(),
                request.allowedLocations()
        );
    }

    @PostMapping("/cv/extract")
    public AiJobFilterExtractionResponse extractCvJobFilter(@RequestBody AiExtractionRequest request) {
        return aiServiceClient.extractCvJobFilter(
                request.text(),
                request.allowedSkills(),
                request.allowedEducationLevels(),
                request.allowedExperienceLevels(),
                request.allowedWorkTypes(),
                request.allowedLocations()
        );
    }

    @PostMapping("/cv/rewrite")
    public CvRewriteResponse rewriteCv(@RequestBody CvRewriteRequest request) {
        return new CvRewriteResponse(aiServiceClient.rewriteCvToProfileText(request.text()));
    }
}
