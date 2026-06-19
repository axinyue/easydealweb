package club.axinyue.easydeal.room;

import java.time.Instant;

public record DealRoom(
        Long id,
        String name,
        Long hostTempUserId,
        String inviteCode,
        Integer inviteMaxUses,
        Integer inviteUsedCount,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
