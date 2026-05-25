package si.um.feri.smartjobs.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import si.um.feri.smartjobs.ai.config.AiProperties;

@SpringBootApplication
@EnableConfigurationProperties(AiProperties.class)
public class AiServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}
