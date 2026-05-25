package si.um.feri.smartjobs.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smartjobs.ai")
public record AiProperties(String serviceUrl) {
}
