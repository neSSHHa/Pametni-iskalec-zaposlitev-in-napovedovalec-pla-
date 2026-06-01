package si.um.feri.smartjobs.config;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestTemplateConfig.class);

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .additionalInterceptors((request, body, execution) -> {
                    String requestId = MDC.get(RequestLoggingFilter.REQUEST_ID_MDC_KEY);
                    String interactionId = MDC.get(RequestLoggingFilter.INTERACTION_ID_MDC_KEY);
                    if (requestId != null && !requestId.isBlank()) {
                        request.getHeaders().set(RequestLoggingFilter.REQUEST_ID_HEADER, requestId);
                    }
                    if (interactionId != null && !interactionId.isBlank()) {
                        request.getHeaders().set(RequestLoggingFilter.INTERACTION_ID_HEADER, interactionId);
                    }

                    String target = request.getURI().getHost();
                    String path = request.getURI().getPath();
                    long startedAt = System.nanoTime();
                    LOGGER.info(
                            "event=outbound.started requestId={} interactionId={} target={} method={} path={}",
                            requestId,
                            interactionId,
                            target,
                            request.getMethod(),
                            path
                    );

                    try {
                        var response = execution.execute(request, body);
                        LOGGER.info(
                                "event=outbound.completed requestId={} interactionId={} target={} method={} path={} status={} durationMs={}",
                                requestId,
                                interactionId,
                                target,
                                request.getMethod(),
                                path,
                                response.getStatusCode().value(),
                                elapsedMs(startedAt)
                        );
                        return response;
                    } catch (IOException | RuntimeException exception) {
                        LOGGER.error(
                                "event=outbound.failed requestId={} interactionId={} target={} method={} path={} durationMs={}",
                                requestId,
                                interactionId,
                                target,
                                request.getMethod(),
                                path,
                                elapsedMs(startedAt),
                                exception
                        );
                        throw exception;
                    }
                })
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(420))
                .build();
    }

    private static long elapsedMs(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }
}
