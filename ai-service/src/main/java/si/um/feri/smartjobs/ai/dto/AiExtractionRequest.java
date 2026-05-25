package si.um.feri.smartjobs.ai.dto;

import java.util.List;

public record AiExtractionRequest(
        String text,
        List<String> allowedSkills,
        List<String> allowedEducationLevels,
        List<String> allowedExperienceLevels,
        List<String> allowedWorkTypes
) {
}
