package si.um.feri.smartjobs.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import si.um.feri.smartjobs.admin.dto.AdminLogEntryDto;
import si.um.feri.smartjobs.admin.dto.AdminLogsResponseDto;
import si.um.feri.smartjobs.admin.dto.AdminOverviewDto;
import si.um.feri.smartjobs.job.repository.JobRepository;
import si.um.feri.smartjobs.user.repository.UserRepository;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin panel", description = "Admin-only endpoints for operational overview screens.")
public class AdminPanelController {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final RestTemplate restTemplate;
    private final String lokiBaseUrl;
    private final String keycloakBaseUrl;
    private final String keycloakAdminUsername;
    private final String keycloakAdminPassword;

    public AdminPanelController(
            UserRepository userRepository,
            JobRepository jobRepository,
            RestTemplate restTemplate,
            @Value("${smartjobs.admin.loki-base-url:http://loki:3100}") String lokiBaseUrl,
            @Value("${smartjobs.admin.keycloak-base-url:http://keycloak:8080}") String keycloakBaseUrl,
            @Value("${smartjobs.admin.keycloak-admin-username:admin}") String keycloakAdminUsername,
            @Value("${smartjobs.admin.keycloak-admin-password:admin}") String keycloakAdminPassword
    ) {
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.restTemplate = restTemplate;
        this.lokiBaseUrl = lokiBaseUrl;
        this.keycloakBaseUrl = keycloakBaseUrl;
        this.keycloakAdminUsername = keycloakAdminUsername;
        this.keycloakAdminPassword = keycloakAdminPassword;
    }

    @GetMapping("/overview")
    @Operation(summary = "Admin overview", description = "Returns a small operational overview for the admin panel.")
    public AdminOverviewDto overview() {
        return new AdminOverviewDto(
                keycloakUserCount(),
                jobRepository.count(),
                "OK"
        );
    }

    @GetMapping("/logs")
    @Operation(summary = "Operational logs", description = "Queries Loki logs for admin diagnostics.")
    public AdminLogsResponseDto logs(
            @RequestParam(defaultValue = "backend") String service,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "backward") String direction,
            @RequestParam(defaultValue = "100") int limit
    ) {
        int safeLimit = Math.max(10, Math.min(limit, 500));
        String safeDirection = "forward".equalsIgnoreCase(direction) ? "forward" : "backward";
        String query = lokiQuery(service, search);
        URI uri = UriComponentsBuilder.fromHttpUrl(lokiBaseUrl)
                .path("/loki/api/v1/query_range")
                .queryParam("query", query)
                .queryParam("start", toEpochNanos(from, Instant.now().minusSeconds(3600)))
                .queryParam("end", toEpochNanos(to, Instant.now()))
                .queryParam("limit", safeLimit)
                .queryParam("direction", safeDirection)
                .build()
                .toUri();

        Map<?, ?> response = restTemplate.getForObject(uri, Map.class);
        return new AdminLogsResponseDto(query, parseLogs(response));
    }

    private String lokiQuery(String service, String search) {
        String serviceRegex = switch (service) {
            case "weekly" -> "weekly-job-updater.*|weekly-job-updater-scheduler.*";
            case "all" -> ".+";
            default -> "backend";
        };
        String query = "{service=~\"" + serviceRegex + "\"}";
        if (search != null && !search.isBlank()) {
            query += " |= " + quote(search.trim());
        }
        return query;
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static long toEpochNanos(String value, Instant fallback) {
        if (value == null || value.isBlank()) {
            return fallback.getEpochSecond() * 1_000_000_000L + fallback.getNano();
        }
        try {
            Instant instant = Instant.parse(value);
            return instant.getEpochSecond() * 1_000_000_000L + instant.getNano();
        } catch (DateTimeParseException ignored) {
            return fallback.getEpochSecond() * 1_000_000_000L + fallback.getNano();
        }
    }

    @SuppressWarnings("unchecked")
    private List<AdminLogEntryDto> parseLogs(Map<?, ?> response) {
        List<AdminLogEntryDto> logs = new ArrayList<>();
        Object data = response == null ? null : response.get("data");
        if (!(data instanceof Map<?, ?> dataMap) || !(dataMap.get("result") instanceof List<?> results)) {
            return logs;
        }

        for (Object result : results) {
            if (!(result instanceof Map<?, ?> streamResult)) continue;
            Map<String, Object> stream = streamResult.get("stream") instanceof Map<?, ?> labels
                    ? (Map<String, Object>) labels
                    : Map.of();
            if (!(streamResult.get("values") instanceof List<?> values)) continue;
            for (Object value : values) {
                if (value instanceof List<?> pair && pair.size() >= 2) {
                    logs.add(new AdminLogEntryDto(
                            formatLokiTimestamp(String.valueOf(pair.get(0))),
                            String.valueOf(stream.getOrDefault("service", "unknown")),
                            String.valueOf(stream.getOrDefault("level", "INFO")),
                            String.valueOf(pair.get(1))
                    ));
                }
            }
        }
        return logs;
    }

    private static String formatLokiTimestamp(String nanos) {
        try {
            long value = Long.parseLong(nanos);
            return Instant.ofEpochSecond(value / 1_000_000_000L, value % 1_000_000_000L).toString();
        } catch (NumberFormatException ignored) {
            return nanos;
        }
    }

    private long keycloakUserCount() {
        try {
            MultiValueMap<String, String> tokenRequest = new LinkedMultiValueMap<>();
            tokenRequest.add("grant_type", "password");
            tokenRequest.add("client_id", "admin-cli");
            tokenRequest.add("username", keycloakAdminUsername);
            tokenRequest.add("password", keycloakAdminPassword);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            Map<?, ?> token = restTemplate.postForObject(
                    keycloakBaseUrl + "/realms/master/protocol/openid-connect/token",
                    new org.springframework.http.HttpEntity<>(tokenRequest, headers),
                    Map.class
            );
            String accessToken = token == null ? null : String.valueOf(token.get("access_token"));
            if (accessToken == null || accessToken.isBlank() || "null".equals(accessToken)) {
                return userRepository.count();
            }

            org.springframework.http.HttpHeaders authHeaders = new org.springframework.http.HttpHeaders();
            authHeaders.setBearerAuth(accessToken);
            org.springframework.http.ResponseEntity<Long> response = restTemplate.exchange(
                    keycloakBaseUrl + "/admin/realms/smartjobs/users/count",
                    org.springframework.http.HttpMethod.GET,
                    new org.springframework.http.HttpEntity<>(authHeaders),
                    Long.class
            );
            return response.getBody() == null ? userRepository.count() : response.getBody();
        } catch (RuntimeException ignored) {
            return userRepository.count();
        }
    }
}
