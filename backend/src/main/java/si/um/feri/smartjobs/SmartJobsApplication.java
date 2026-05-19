package si.um.feri.smartjobs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import si.um.feri.smartjobs.ai.config.AiProperties;

@SpringBootApplication
@EnableConfigurationProperties(AiProperties.class)
@EnableScheduling
public class SmartJobsApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartJobsApplication.class, args);
    }
}
