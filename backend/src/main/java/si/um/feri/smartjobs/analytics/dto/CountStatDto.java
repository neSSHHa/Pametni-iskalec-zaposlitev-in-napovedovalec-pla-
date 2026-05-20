package si.um.feri.smartjobs.analytics.dto;

public record CountStatDto(
        String label,
        long count,
        double percentage
) {
}
