package si.um.feri.smartjobs.auth.dto;

import java.util.List;

public record AuthResponse(
        String id,
        String name,
        String email,
        List<String> roles
) {
}
