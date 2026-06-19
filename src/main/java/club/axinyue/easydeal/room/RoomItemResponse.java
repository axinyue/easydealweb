package club.axinyue.easydeal.room;

import java.time.Instant;
import java.util.List;

public record RoomItemResponse(
        Long id,
        String title,
        String description,
        String saleMode,
        Boolean biddingOpen,
        RoomBidResponse topBid,
        List<RoomBidResponse> bids,
        Instant createdAt
) {
}
