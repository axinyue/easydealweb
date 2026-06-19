package club.axinyue.easydeal.room;

public record CreateRoomItemRequest(
        String title,
        String description,
        String saleMode
) {
}
