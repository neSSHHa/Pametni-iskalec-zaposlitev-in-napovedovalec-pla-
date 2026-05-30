package si.um.feri.smartjobs.salary.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smartjobs.salary")
public record SalaryProperties(String serviceUrl) {
}
