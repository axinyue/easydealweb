package club.axinyue.easydeal.room;

import java.time.Instant;

public record RoomMemberResponse(
        Long id,
        Long tempUserId,
        Integer memberNo,
        String role,
        String username,
        Instant joinedAt
) {
}
