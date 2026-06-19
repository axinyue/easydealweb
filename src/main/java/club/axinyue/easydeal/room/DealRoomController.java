package club.axinyue.easydeal.room;

import club.axinyue.easydeal.tempuser.TempUserResponse;
import club.axinyue.easydeal.tempuser.TempUserService;
import club.axinyue.easydeal.tempuser.TempUserSession;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
public class DealRoomController {
    private final DealRoomService dealRoomService;
    private final TempUserService tempUserService;

    public DealRoomController(DealRoomService dealRoomService, TempUserService tempUserService) {
        this.dealRoomService = dealRoomService;
        this.tempUserService = tempUserService;
    }

    @PostMapping
    public DealRoomResponse createRoom(
            @RequestBody(required = false) CreateRoomRequest request,
            @CookieValue(value = TempUserService.TOKEN_COOKIE_NAME, required = false) String token,
            @RequestHeader(name = "Accept-Language", required = false) String acceptLanguage,
            HttpServletResponse response
    ) {
        TempUserResponse tempUser = currentTempUser(token, acceptLanguage, response);
        return dealRoomService.createRoom(tempUser, request == null ? new CreateRoomRequest(null) : request);
    }

    @PostMapping("/join")
    public DealRoomResponse joinRoom(
            @RequestBody JoinRoomRequest request,
            @CookieValue(value = TempUserService.TOKEN_COOKIE_NAME, required = false) String token,
            @RequestHeader(name = "Accept-Language", required = false) String acceptLanguage,
            HttpServletResponse response
    ) {
        TempUserResponse tempUser = currentTempUser(token, acceptLanguage, response);
        return dealRoomService.joinRoom(tempUser, request);
    }

    @PostMapping("/{roomId}/invites")
    public DealRoomResponse createInviteCodes(
            @PathVariable Long roomId,
            @RequestBody(required = false) CreateInviteCodesRequest request,
            @CookieValue(value = TempUserService.TOKEN_COOKIE_NAME, required = false) String token,
            @RequestHeader(name = "Accept-Language", required = false) String acceptLanguage,
            HttpServletResponse response
    ) {
        TempUserResponse tempUser = currentTempUser(token, acceptLanguage, response);
        return dealRoomService.createInviteCodes(tempUser, roomId, request);
    }

    @PostMapping("/{roomId}/invites/default/reset")
    public DealRoomResponse resetDefaultInviteCode(
            @PathVariable Long roomId,
            @CookieValue(value = TempUserService.TOKEN_COOKIE_NAME, required = false) String token,
            @RequestHeader(name = "Accept-Language", required = false) String acceptLanguage,
            HttpServletResponse response
    ) {
        TempUserResponse tempUser = currentTempUser(token, acceptLanguage, response);
        return dealRoomService.resetDefaultInviteCode(tempUser, roomId);
    }

    @DeleteMapping("/{roomId}/members/{memberId}")
    public DealRoomResponse removeMember(
            @PathVariable Long roomId,
            @PathVariable Long memberId,
            @CookieValue(value = TempUserService.TOKEN_COOKIE_NAME, required = false) String token,
            @RequestHeader(name = "Accept-Language", required = false) String acceptLanguage,
            HttpServletResponse response
    ) {
        TempUserResponse tempUser = currentTempUser(token, acceptLanguage, response);
        return dealRoomService.removeMember(tempUser, roomId, memberId);
    }

    @PostMapping("/{roomId}/settings")
    public DealRoomResponse updateRoomSettings(
            @PathVariable Long roomId,
            @RequestBody(required = false) UpdateRoomSettingsRequest request,
            @CookieValue(value = TempUserService.TOKEN_COOKIE_NAME, required = false) String token,
            @RequestHeader(name = "Accept-Language", required = false) String acceptLanguage,
            HttpServletResponse response
    ) {
        TempUserResponse tempUser = currentTempUser(token, acceptLanguage, response);
        return dealRoomService.updateRoomSettings(tempUser, roomId, request);
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> clearRoom(
            @PathVariable Long roomId,
            @CookieValue(value = TempUserService.TOKEN_COOKIE_NAME, required = false) String token,
            @RequestHeader(name = "Accept-Language", required = false) String acceptLanguage,
            HttpServletResponse response
    ) {
        TempUserResponse tempUser = currentTempUser(token, acceptLanguage, response);
        dealRoomService.clearRoom(tempUser, roomId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{roomId}")
    public DealRoomResponse getRoom(
            @PathVariable Long roomId,
            @CookieValue(value = TempUserService.TOKEN_COOKIE_NAME, required = false) String token,
            @RequestHeader(name = "Accept-Language", required = false) String acceptLanguage,
            HttpServletResponse response
    ) {
        TempUserResponse tempUser = currentTempUser(token, acceptLanguage, response);
        return dealRoomService.snapshotForMember(roomId, tempUser.id());
    }

    @PostMapping("/{roomId}/items")
    public DealRoomResponse addItem(
            @PathVariable Long roomId,
            @RequestBody(required = false) CreateRoomItemRequest request,
            @CookieValue(value = TempUserService.TOKEN_COOKIE_NAME, required = false) String token,
            @RequestHeader(name = "Accept-Language", required = false) String acceptLanguage,
            HttpServletResponse response
    ) {
        TempUserResponse tempUser = currentTempUser(token, acceptLanguage, response);
        CreateRoomItemRequest safeRequest = request == null
                ? new CreateRoomItemRequest(null, null, null)
                : request;
        return dealRoomService.addItem(tempUser, roomId, safeRequest);
    }

    @PostMapping("/{roomId}/items/{itemId}/bids")
    public DealRoomResponse placeBid(
            @PathVariable Long roomId,
            @PathVariable Long itemId,
            @RequestBody CreateBidRequest request,
            @CookieValue(value = TempUserService.TOKEN_COOKIE_NAME, required = false) String token,
            @RequestHeader(name = "Accept-Language", required = false) String acceptLanguage,
            HttpServletResponse response
    ) {
        TempUserResponse tempUser = currentTempUser(token, acceptLanguage, response);
        return dealRoomService.placeBid(tempUser, roomId, itemId, request);
    }

    @PostMapping("/{roomId}/items/{itemId}/close")
    public DealRoomResponse closeBidding(
            @PathVariable Long roomId,
            @PathVariable Long itemId,
            @CookieValue(value = TempUserService.TOKEN_COOKIE_NAME, required = false) String token,
            @RequestHeader(name = "Accept-Language", required = false) String acceptLanguage,
            HttpServletResponse response
    ) {
        TempUserResponse tempUser = currentTempUser(token, acceptLanguage, response);
        return dealRoomService.closeBidding(tempUser, roomId, itemId);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    private TempUserResponse currentTempUser(String token, String acceptLanguage, HttpServletResponse response) {
        TempUserSession session = tempUserService.getOrCreate(token, resolveLocale(acceptLanguage));
        if (session.hasNewCookie()) {
            response.addHeader(HttpHeaders.SET_COOKIE, session.cookie().toString());
        }
        return session.tempUser();
    }

    private Locale resolveLocale(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return Locale.CHINESE;
        }

        try {
            List<Locale.LanguageRange> ranges = Locale.LanguageRange.parse(acceptLanguage);
            if (ranges.isEmpty()) {
                return Locale.CHINESE;
            }
            return Locale.forLanguageTag(ranges.get(0).getRange());
        } catch (IllegalArgumentException e) {
            return Locale.CHINESE;
        }
    }
}
