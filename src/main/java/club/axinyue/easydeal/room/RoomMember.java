package club.axinyue.easydeal.room;

import java.time.Instant;

public record RoomMember(
        Long id,
        Long roomId,
        Long tempUserId,
        Integer memberNo,
        String role,
        String username,
        Instant joinedAt
) {
}
