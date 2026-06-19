package club.axinyue.easydeal.tempuser;

public record TempUserResponse(
        Long id,
        String username,
        String locale
) {
    public static TempUserResponse from(TempUser tempUser) {
        return new TempUserResponse(tempUser.id(), tempUser.username(), tempUser.locale());
    }
}
