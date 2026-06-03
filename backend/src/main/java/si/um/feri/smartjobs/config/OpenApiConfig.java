package si.um.feri.smartjobs.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI smartJobsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SmartJobs API")
                        .version("1.0.0")
                        .description("""
                                SmartJobs is a job search and salary prediction platform for exploring job postings,
                                filtering opportunities with structured criteria, using AI-assisted natural language
                                search, extracting CV content, matching CV profiles to jobs, and viewing labor-market
                                analytics.
                                """)
                        .contact(new Contact()
                                .name("SmartJobs Project Team")
                                .url("https://jobsearchwith.me"))
                        .license(new License()
                                .name("Academic project")))
                .addServersItem(new Server()
                        .url("https://jobsearchwith.me")
                        .description("Production server"))
                .addServersItem(new Server()
                        .url("http://localhost:8080")
                        .description("Local development server"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Optional JWT bearer token. Enter only the token value or use the Bearer prefix.")));
    }
}
