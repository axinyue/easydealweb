package club.axinyue.easydeal.room;

import java.math.BigDecimal;
import java.time.Instant;

public record RoomBidResponse(
        Long id,
        Long memberId,
        Integer memberNo,
        String username,
        BigDecimal amount,
        Instant createdAt
) {
}
