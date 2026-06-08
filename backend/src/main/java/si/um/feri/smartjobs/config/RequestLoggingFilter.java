package si.um.feri.smartjobs.config;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
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
    public static final String USER_ID_MDC_KEY = "userId";
    public static final String USER_EMAIL_MDC_KEY = "userEmail";

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{1,100}");
    private static final String GUEST = "guest";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtDecoder jwtDecoder;

    public RequestLoggingFilter(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = requestId(request.getHeader(REQUEST_ID_HEADER));
        String interactionId = interactionId(request.getHeader(INTERACTION_ID_HEADER), requestId);
        UserLogContext user = userContext(request);

        long startedAt = System.nanoTime();
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        MDC.put(INTERACTION_ID_MDC_KEY, interactionId);
        MDC.put(USER_ID_MDC_KEY, user.userId());
        MDC.put(USER_EMAIL_MDC_KEY, user.userEmail());
        response.setHeader(REQUEST_ID_HEADER, requestId);
        response.setHeader(INTERACTION_ID_HEADER, interactionId);
        LOGGER.info(
                "event=request.started requestId={} interactionId={} userId={} userEmail={} method={} path={}",
                requestId,
                interactionId,
                user.userId(),
                user.userEmail(),
                request.getMethod(),
                request.getRequestURI()
        );

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            LOGGER.info(
                    "event=request.completed requestId={} interactionId={} userId={} userEmail={} method={} path={} status={} durationMs={}",
                    requestId,
                    interactionId,
                    user.userId(),
                    user.userEmail(),
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs
            );
            MDC.remove(REQUEST_ID_MDC_KEY);
            MDC.remove(INTERACTION_ID_MDC_KEY);
            MDC.remove(USER_ID_MDC_KEY);
            MDC.remove(USER_EMAIL_MDC_KEY);
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

    private UserLogContext userContext(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return UserLogContext.guest();
        }

        try {
            Jwt jwt = jwtDecoder.decode(authorization.substring(BEARER_PREFIX.length()));
            return new UserLogContext(
                    valueOrGuest(jwt.getSubject()),
                    valueOrGuest(jwt.getClaimAsString("email"))
            );
        } catch (RuntimeException ignored) {
            return UserLogContext.guest();
        }
    }

    private String valueOrGuest(String value) {
        return value == null || value.isBlank() ? GUEST : value;
    }

    private record UserLogContext(String userId, String userEmail) {
        static UserLogContext guest() {
            return new UserLogContext(GUEST, GUEST);
        }
    }
}
