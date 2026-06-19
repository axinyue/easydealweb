package club.axinyue.easydeal.room;

public record CreateInviteCodesRequest(
        String inviteType,
        Integer maxUses,
        Integer quantity
) {
}
