package club.axinyue.easydeal.room;

import java.time.Instant;
import java.util.List;

public record DealRoomResponse(
        Long id,
        String name,
        Long hostTempUserId,
        String inviteCode,
        Integer inviteMaxUses,
        Integer inviteUsedCount,
        String status,
        List<RoomInviteCodeResponse> inviteCodes,
        List<RoomMemberResponse> members,
        List<RoomItemResponse> items,
        Instant createdAt
) {
}
