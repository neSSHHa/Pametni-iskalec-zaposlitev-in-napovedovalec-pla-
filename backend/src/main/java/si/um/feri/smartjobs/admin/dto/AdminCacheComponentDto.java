package si.um.feri.smartjobs.admin.dto;

import java.util.Map;

public record AdminCacheComponentDto(
        String name,
        boolean loaded,
        Map<String, Integer> details
) {
}
