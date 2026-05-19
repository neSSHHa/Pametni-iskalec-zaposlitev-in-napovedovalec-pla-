package si.um.feri.smartjobs.ai.dto;

import si.um.feri.smartjobs.job.dto.JobFilterRequest;

public record AiJobFilterDebugResponse(
        AiJobFilterExtractionResponse rawAiResponse,
        JobFilterRequest filterRequest
) {
}
