package si.um.feri.smartjobs.admin.dto;

import java.util.List;

public record AdminCacheStatusDto(
        boolean allLoaded,
        List<AdminCacheComponentDto> components
) {
}
