package si.um.feri.smartjobs.auth.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import si.um.feri.smartjobs.auth.dto.AuthResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
    public AuthResponse me(@AuthenticationPrincipal Jwt jwt) {
        return new AuthResponse(
                jwt.getSubject(),
                jwt.getClaimAsString("name"),
                jwt.getClaimAsString("email"),
                roles(jwt)
        );
    }

    @SuppressWarnings("unchecked")
    private List<String> roles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof List<?> values)) {
            return List.of();
        }

        List<String> roles = new ArrayList<>();
        for (Object value : values) {
            roles.add(String.valueOf(value));
        }
        return roles;
    }
}
