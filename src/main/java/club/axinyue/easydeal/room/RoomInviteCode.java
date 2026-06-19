package club.axinyue.easydeal.room;

import java.time.Instant;

public record RoomInviteCode(
        Long id,
        Long roomId,
        String code,
        String inviteType,
        Integer maxUses,
        Integer usedCount,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
