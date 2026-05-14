package si.um.feri.smartjobs.job.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import si.um.feri.smartjobs.job.entity.Job;
import si.um.feri.smartjobs.job.repository.JobRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Configuration
public class JobDataSeeder {
    @Bean
    CommandLineRunner seedJobs(JobRepository jobRepository) {
        return args -> {
            if (jobRepository.count() > 0) {
                return;
            }

            LocalDate today = LocalDate.now();
            jobRepository.saveAll(List.of(
                    new Job(
                            "job-1",
                            "Maribor Digital Lab",
                            "Junior Java Developer",
                            "Razvoj REST API-jev in integracija sa MySQL bazom.",
                            1,
                            BigDecimal.valueOf(2600),
                            BigDecimal.valueOf(1800),
                            "https://example.com/jobs/1",
                            today.minusDays(2),
                            today,
                            today,
                            BigDecimal.valueOf(1800),
                            BigDecimal.valueOf(2600),
                            null,
                            null
                    ),
                    new Job(
                            "job-2",
                            "Ljubljana Product Studio",
                            "React Frontend Engineer",
                            "Izrada preglednog React interfejsa za prikaz poslova.",
                            2,
                            BigDecimal.valueOf(3600),
                            BigDecimal.valueOf(2400),
                            "https://example.com/jobs/2",
                            today.minusDays(4),
                            today,
                            today,
                            BigDecimal.valueOf(2400),
                            BigDecimal.valueOf(3600),
                            null,
                            null
                    )
            ));
        };
    }
}
