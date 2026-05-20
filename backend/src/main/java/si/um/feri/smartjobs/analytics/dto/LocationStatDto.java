package si.um.feri.smartjobs.analytics.dto;

import java.math.BigDecimal;

public record LocationStatDto(
        String label,
        String city,
        String region,
        String country,
        long count,
        double percentage,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
