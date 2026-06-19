package club.axinyue.easydeal.tempuser;

import java.time.Instant;

public record TempUser(
        Long id,
        String tokenHash,
        String username,
        String locale,
        Instant createdAt,
        Instant updatedAt
) {
}
