package club.axinyue.easydeal.room;

import club.axinyue.easydeal.tempuser.TempUserResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DealRoomService {
    private static final String DEFAULT_ROOM_NAME = "临时交易房间";
    private static final String ROOM_NAME_SUFFIX = "号房间";
    private static final String ITEM_TITLE_SUFFIX = "号商品";
    private static final String DEFAULT_SALE_MODE = "AUCTION";
    private static final String SALE_MODE_AUCTION = "AUCTION";
    private static final String SALE_MODE_TENDER = "TENDER";
    private static final String INVITE_TYPE_ONE_PERSON = "ONE_PERSON";
    private static final String INVITE_TYPE_ROOM_SHARED = "ROOM_SHARED";
    private static final String INVITE_TYPE_MULTI_PERSON = "MULTI_PERSON";
    private static final int DEFAULT_ROOM_INVITE_MAX_USES = 50;
    private static final int MAX_INVITE_QUANTITY = 50;
    private static final int MAX_INVITE_USES = 500;
    private static final String INVITE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Set<String> INVITE_TYPES = Set.of(
            INVITE_TYPE_ONE_PERSON,
            INVITE_TYPE_ROOM_SHARED,
            INVITE_TYPE_MULTI_PERSON
    );
    private static final Set<String> SALE_MODES = Set.of(SALE_MODE_AUCTION, SALE_MODE_TENDER);

    private final SecureRandom secureRandom = new SecureRandom();
    private final DealRoomRepository dealRoomRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public DealRoomService(DealRoomRepository dealRoomRepository, SimpMessagingTemplate messagingTemplate) {
        this.dealRoomRepository = dealRoomRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public DealRoomResponse createRoom(TempUserResponse tempUser, CreateRoomRequest request) {
        DealRoom room = dealRoomRepository.createRoom(DEFAULT_ROOM_NAME, tempUser.id(), generateUniqueInviteCode());
        dealRoomRepository.updateRoomName(room.id(), room.id() + ROOM_NAME_SUFFIX);
        dealRoomRepository.createInviteCode(room.id(), room.inviteCode(), INVITE_TYPE_ROOM_SHARED, DEFAULT_ROOM_INVITE_MAX_USES);
        dealRoomRepository.addMember(room.id(), tempUser.id(), "HOST");
        return adminSnapshot(room.id());
    }

    @Transactional
    public DealRoomResponse joinRoom(TempUserResponse tempUser, JoinRoomRequest request) {
        if (request == null || (request.roomId() == null && !StringUtils.hasText(request.inviteCode()))) {
            throw new IllegalArgumentException("房间编号或邀请码不能为空");
        }

        RoomInviteCode inviteCode = findInviteCodeForJoin(request);
        DealRoom room = inviteCode == null
                ? findRoomForJoin(request)
                : dealRoomRepository.findRoomById(inviteCode.roomId())
                .orElseThrow(() -> new IllegalArgumentException("房间不存在"));
        if (request.roomId() != null && !request.roomId().equals(room.id())) {
            throw new IllegalArgumentException("邀请码与房间不匹配");
        }

        if (dealRoomRepository.findMember(room.id(), tempUser.id()).isPresent()) {
            return snapshotForMember(room.id(), tempUser.id());
        }

        if (!"OPEN".equals(room.status())) {
            throw new IllegalStateException("房间已停止新用户进入");
        }

        if (inviteCode != null) {
            requireUsableInviteCode(inviteCode);
        }

        RoomMember member = dealRoomRepository.addMember(room.id(), tempUser.id(), "PARTICIPANT");
        if (inviteCode != null) {
            dealRoomRepository.recordInviteCodeUsage(inviteCode, tempUser.id(), member.id());
        }
        dealRoomRepository.refreshLegacyInviteUsage(room.id());
        broadcast(room.id());
        return snapshotForMember(room.id(), tempUser.id());
    }

    private DealRoom findRoomForJoin(JoinRoomRequest request) {
        if (request.roomId() == null) {
            throw new IllegalArgumentException("邀请码不存在");
        }
        return dealRoomRepository.findRoomById(request.roomId())
                .orElseThrow(() -> new IllegalArgumentException("房间不存在"));
    }

    private RoomInviteCode findInviteCodeForJoin(JoinRoomRequest request) {
        if (!StringUtils.hasText(request.inviteCode())) {
            return null;
        }
        return dealRoomRepository.lockInviteCodeByCode(request.inviteCode().trim())
                .orElseThrow(() -> new IllegalArgumentException("邀请码不存在"));
    }

    private void requireUsableInviteCode(RoomInviteCode inviteCode) {
        if (!"ACTIVE".equals(inviteCode.status())) {
            throw new IllegalStateException("邀请码已停用");
        }
        if (inviteCode.usedCount() >= inviteCode.maxUses()) {
            throw new IllegalStateException("邀请码使用次数已达上限");
        }
    }

    @Transactional
    public DealRoomResponse createInviteCodes(TempUserResponse tempUser, Long roomId, CreateInviteCodesRequest request) {
        RoomMember member = requireMember(roomId, tempUser.id());
        requireHost(member);

        String inviteType = normalizeInviteType(request == null ? null : request.inviteType());
        int quantity = normalizeQuantity(request == null ? null : request.quantity());
        int maxUses = normalizeMaxUses(inviteType, request == null ? null : request.maxUses());

        List<String> codes = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            codes.add(generateUniqueInviteCode());
        }
        for (String code : codes) {
            dealRoomRepository.createInviteCode(roomId, code, inviteType, maxUses);
        }

        DealRoomResponse snapshot = adminSnapshot(roomId);
        broadcast(roomId);
        return snapshot;
    }

    @Transactional
    public DealRoomResponse resetDefaultInviteCode(TempUserResponse tempUser, Long roomId) {
        RoomMember member = requireMember(roomId, tempUser.id());
        requireHost(member);

        dealRoomRepository.resetDefaultInviteCode(roomId, generateUniqueInviteCode(), DEFAULT_ROOM_INVITE_MAX_USES);
        DealRoomResponse snapshot = adminSnapshot(roomId);
        broadcast(roomId);
        return snapshot;
    }

    @Transactional
    public DealRoomResponse removeMember(TempUserResponse tempUser, Long roomId, Long memberId) {
        RoomMember operator = requireMember(roomId, tempUser.id());
        requireHost(operator);

        RoomMember target = dealRoomRepository.findMemberById(roomId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("成员不存在"));
        if ("HOST".equals(target.role())) {
            throw new IllegalStateException("不能移除房主");
        }

        dealRoomRepository.removeMember(roomId, memberId);
        dealRoomRepository.refreshLegacyInviteUsage(roomId);
        DealRoomResponse snapshot = adminSnapshot(roomId);
        broadcast(roomId);
        messagingTemplate.convertAndSendToUser(
                "temp-user-" + target.tempUserId(),
                "/queue/rooms/" + roomId,
                snapshot(roomId, false)
        );
        return snapshot;
    }

    @Transactional
    public DealRoomResponse updateRoomSettings(TempUserResponse tempUser, Long roomId, UpdateRoomSettingsRequest request) {
        RoomMember member = requireMember(roomId, tempUser.id());
        requireHost(member);

        boolean allowNewMembers = request == null || !Boolean.FALSE.equals(request.allowNewMembers());
        dealRoomRepository.updateRoomStatus(roomId, allowNewMembers ? "OPEN" : "CLOSED");
        DealRoomResponse snapshot = adminSnapshot(roomId);
        broadcast(roomId);
        return snapshot;
    }

    @Transactional
    public void clearRoom(TempUserResponse tempUser, Long roomId) {
        RoomMember member = requireMember(roomId, tempUser.id());
        requireHost(member);

        List<Long> tempUserIds = dealRoomRepository.findRoomTempUserIds(roomId);
        broadcastRoomCleared(roomId, tempUserIds);
        dealRoomRepository.clearRoom(roomId);
        dealRoomRepository.deleteUnusedTempUsers(tempUserIds);
    }

    private String normalizeInviteType(String inviteType) {
        String normalized = StringUtils.hasText(inviteType)
                ? inviteType.trim().toUpperCase()
                : INVITE_TYPE_ONE_PERSON;
        if (!INVITE_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("邀请码类型不正确");
        }
        return normalized;
    }

    private int normalizeQuantity(Integer quantity) {
        int normalized = quantity == null ? 1 : quantity;
        if (normalized < 1 || normalized > MAX_INVITE_QUANTITY) {
            throw new IllegalArgumentException("生成数量必须在1到50之间");
        }
        return normalized;
    }

    private int normalizeMaxUses(String inviteType, Integer maxUses) {
        int normalized = INVITE_TYPE_ONE_PERSON.equals(inviteType) ? 1 : (maxUses == null ? 1 : maxUses);
        if (INVITE_TYPE_ONE_PERSON.equals(inviteType)) {
            return 1;
        }
        if (normalized < 1 || normalized > MAX_INVITE_USES) {
            throw new IllegalArgumentException("可使用次数必须在1到500之间");
        }
        return normalized;
    }

    @Transactional(readOnly = true)
    public DealRoomResponse adminSnapshot(Long roomId) {
        return snapshot(roomId, true);
    }

    @Transactional(readOnly = true)
    public DealRoomResponse snapshotForMember(Long roomId, Long tempUserId) {
        RoomMember member = requireMember(roomId, tempUserId);
        return snapshot(roomId, "HOST".equals(member.role()));
    }

    private DealRoomResponse snapshot(Long roomId, boolean includeInviteCodes) {
        DealRoom room = dealRoomRepository.findRoomById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("房间不存在"));
        RoomInviteCodeResponse primaryInviteCode = dealRoomRepository.findPrimaryInviteCode(room.id()).orElse(null);
        return new DealRoomResponse(
                room.id(),
                room.name(),
                room.hostTempUserId(),
                primaryInviteCode == null ? room.inviteCode() : primaryInviteCode.code(),
                primaryInviteCode == null ? room.inviteMaxUses() : primaryInviteCode.maxUses(),
                primaryInviteCode == null ? room.inviteUsedCount() : primaryInviteCode.usedCount(),
                room.status(),
                includeInviteCodes ? dealRoomRepository.findInviteCodes(room.id()) : List.of(),
                dealRoomRepository.findMembers(room.id()),
                dealRoomRepository.findItems(room.id()),
                room.createdAt()
        );
    }

    @Transactional
    public DealRoomResponse addItem(TempUserResponse tempUser, Long roomId, CreateRoomItemRequest request) {
        RoomMember member = requireMember(roomId, tempUser.id());
        requireHost(member);

        String title = (dealRoomRepository.countItems(roomId) + 1) + ITEM_TITLE_SUFFIX;
        String saleMode = normalizeSaleMode(request.saleMode());
        dealRoomRepository.createItem(roomId, member.id(), title, null, saleMode);
        DealRoomResponse snapshot = adminSnapshot(roomId);
        broadcast(roomId);
        return snapshot;
    }

    @Transactional
    public DealRoomResponse placeBid(TempUserResponse tempUser, Long roomId, Long itemId, CreateBidRequest request) {
        RoomMember member = requireMember(roomId, tempUser.id());
        RoomItem item = dealRoomRepository.findItem(roomId, itemId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (!Boolean.TRUE.equals(item.biddingOpen())) {
            throw new IllegalStateException("商品已关闭出价");
        }
        BigDecimal amount = request == null ? null : request.amount();
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("出价金额必须大于0");
        }

        // 拍卖模式会实时展示领先者；竞标模式由快照层控制，关闭后才展示最终结果。
        dealRoomRepository.createBid(roomId, itemId, member.id(), amount);
        broadcast(roomId);
        return snapshotForMember(roomId, tempUser.id());
    }

    @Transactional
    public DealRoomResponse closeBidding(TempUserResponse tempUser, Long roomId, Long itemId) {
        RoomMember member = requireMember(roomId, tempUser.id());
        requireHost(member);
        dealRoomRepository.closeBidding(roomId, itemId);
        DealRoomResponse snapshot = adminSnapshot(roomId);
        broadcast(roomId);
        return snapshot;
    }

    private RoomMember requireMember(Long roomId, Long tempUserId) {
        return dealRoomRepository.findMember(roomId, tempUserId)
                .orElseThrow(() -> new IllegalStateException("请先加入房间"));
    }

    private void requireHost(RoomMember member) {
        if (!"HOST".equals(member.role())) {
            throw new IllegalStateException("只有房主可以执行该操作");
        }
    }

    private String normalizeSaleMode(String saleMode) {
        String normalized = StringUtils.hasText(saleMode)
                ? saleMode.trim().toUpperCase()
                : DEFAULT_SALE_MODE;
        if (!SALE_MODES.contains(normalized)) {
            throw new IllegalArgumentException("出价模式不正确");
        }
        return normalized;
    }

    private void broadcast(Long roomId) {
        DealRoomResponse adminSnapshot = adminSnapshot(roomId);
        DealRoomResponse participantSnapshot = snapshot(roomId, false);
        for (RoomMemberResponse member : dealRoomRepository.findMembers(roomId)) {
            DealRoomResponse memberSnapshot = "HOST".equals(member.role()) ? adminSnapshot : participantSnapshot;
            messagingTemplate.convertAndSendToUser(
                    "temp-user-" + member.tempUserId(),
                    "/queue/rooms/" + roomId,
                    memberSnapshot
            );
        }
    }

    private void broadcastRoomCleared(Long roomId, List<Long> tempUserIds) {
        Map<String, Object> event = Map.of(
                "type", "ROOM_CLEARED",
                "roomId", roomId,
                "message", "房间已被房主清理，请退出。"
        );
        for (Long tempUserId : tempUserIds) {
            messagingTemplate.convertAndSendToUser(
                    "temp-user-" + tempUserId,
                    "/queue/rooms/" + roomId + "/events",
                    event
            );
        }
    }

    private String generateInviteCode() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            builder.append(INVITE_ALPHABET.charAt(secureRandom.nextInt(INVITE_ALPHABET.length())));
        }
        return builder.toString();
    }

    private String generateUniqueInviteCode() {
        String code;
        do {
            code = generateInviteCode();
        } while (dealRoomRepository.inviteCodeExists(code));
        return code;
    }
}
