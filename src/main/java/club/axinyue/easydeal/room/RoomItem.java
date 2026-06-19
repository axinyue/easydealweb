package club.axinyue.easydeal.room;

import java.time.Instant;

public record RoomItem(
        Long id,
        Long roomId,
        String title,
        String description,
        String saleMode,
        Boolean biddingOpen,
        Long createdByMemberId,
        Instant createdAt,
        Instant updatedAt
) {
}
