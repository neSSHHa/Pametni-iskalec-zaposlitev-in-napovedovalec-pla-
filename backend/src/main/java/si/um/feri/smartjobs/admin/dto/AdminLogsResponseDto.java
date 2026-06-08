package si.um.feri.smartjobs.admin.dto;

import java.util.List;

public record AdminLogsResponseDto(
        String query,
        List<AdminLogEntryDto> logs
) {
}
