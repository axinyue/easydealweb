package club.axinyue.easydeal.room;

public record JoinRoomRequest(
        Long roomId,
        String inviteCode
) {
}
