package si.um.feri.smartjobs.admin.dto;

public record AdminLogEntryDto(
        String timestamp,
        String service,
        String level,
        String message
) {
}
