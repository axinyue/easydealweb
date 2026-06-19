package club.axinyue.easydeal.room;

import java.time.Instant;

public record RoomInviteCodeResponse(
        Long id,
        String code,
        String inviteType,
        Integer maxUses,
        Integer usedCount,
        String status,
        Instant createdAt
) {
}
