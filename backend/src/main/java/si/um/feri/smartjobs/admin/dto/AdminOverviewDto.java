package si.um.feri.smartjobs.admin.dto;

public record AdminOverviewDto(
        long users,
        long jobs,
        String status
) {
}
