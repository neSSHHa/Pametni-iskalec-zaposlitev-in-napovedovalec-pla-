package si.um.feri.smartjobs.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import si.um.feri.smartjobs.admin.dto.AdminOverviewDto;
import si.um.feri.smartjobs.job.repository.JobRepository;
import si.um.feri.smartjobs.user.repository.UserRepository;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin panel", description = "Admin-only endpoints for operational overview screens.")
public class AdminPanelController {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;

    public AdminPanelController(UserRepository userRepository, JobRepository jobRepository) {
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
    }

    @GetMapping("/overview")
    @Operation(summary = "Admin overview", description = "Returns a small operational overview for the admin panel.")
    public AdminOverviewDto overview() {
        return new AdminOverviewDto(
                userRepository.count(),
                jobRepository.count(),
                "OK"
        );
    }
}
