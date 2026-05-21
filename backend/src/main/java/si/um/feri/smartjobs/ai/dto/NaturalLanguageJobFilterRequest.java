package si.um.feri.smartjobs.ai.dto;

//  request, ki ga frontend/Postman pošlje AI endpointu
public record NaturalLanguageJobFilterRequest(
        String text,
        String mode
) {
}
