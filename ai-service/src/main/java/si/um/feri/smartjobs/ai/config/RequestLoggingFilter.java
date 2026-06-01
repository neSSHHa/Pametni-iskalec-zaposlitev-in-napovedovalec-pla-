package si.um.feri.smartjobs.ai.config;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String REQUEST_ID_MDC_KEY = "requestId";
    public static final String INTERACTION_ID_HEADER = "X-Interaction-ID";
    public static final String INTERACTION_ID_MDC_KEY = "interactionId";

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{1,100}");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = requestId(request.getHeader(REQUEST_ID_HEADER));
        String interactionId = interactionId(request.getHeader(INTERACTION_ID_HEADER), requestId);

        long startedAt = System.nanoTime();
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        MDC.put(INTERACTION_ID_MDC_KEY, interactionId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        response.setHeader(INTERACTION_ID_HEADER, interactionId);
        LOGGER.info(
                "event=request.started requestId={} interactionId={} method={} path={}",
                requestId,
                interactionId,
                request.getMethod(),
                request.getRequestURI()
        );

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            LOGGER.info(
                    "event=request.completed requestId={} interactionId={} method={} path={} status={} durationMs={}",
                    requestId,
                    interactionId,
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs
            );
            MDC.remove(REQUEST_ID_MDC_KEY);
            MDC.remove(INTERACTION_ID_MDC_KEY);
        }
    }

    private String requestId(String candidate) {
        return candidate != null && SAFE_REQUEST_ID.matcher(candidate).matches()
                ? candidate
                : UUID.randomUUID().toString();
    }

    private String interactionId(String candidate, String requestId) {
        return candidate != null && SAFE_REQUEST_ID.matcher(candidate).matches()
                ? candidate
                : requestId;
    }
}
