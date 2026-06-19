package club.axinyue.easydeal.room;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class DealRoomRepository {
    private final JdbcTemplate jdbcTemplate;

    public DealRoomRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DealRoom createRoom(String name, Long hostTempUserId, String inviteCode) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO deal_rooms (name, host_temp_user_id, invite_code)
                    VALUES (?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, name);
            statement.setLong(2, hostTempUserId);
            statement.setString(3, inviteCode);
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        Long id = key == null ? null : key.longValue();
        Instant now = Instant.now();
        return new DealRoom(id, name, hostTempUserId, inviteCode, 50, 0, "OPEN", now, now);
    }

    public void updateRoomName(Long roomId, String name) {
        jdbcTemplate.update("""
                UPDATE deal_rooms
                SET name = ?
                WHERE id = ?
                """, name, roomId);
    }

    public void createInviteCode(Long roomId, String code, String inviteType, Integer maxUses) {
        jdbcTemplate.update("""
                INSERT INTO deal_room_invite_codes (room_id, code, invite_type, max_uses)
                VALUES (?, ?, ?, ?)
                """, roomId, code, inviteType, maxUses);
    }

    public void resetDefaultInviteCode(Long roomId, String code, Integer maxUses) {
        jdbcTemplate.update("""
                UPDATE deal_room_invite_codes
                SET status = 'DISABLED'
                WHERE room_id = ? AND invite_type = 'ROOM_SHARED' AND status = 'ACTIVE'
                """, roomId);
        createInviteCode(roomId, code, "ROOM_SHARED", maxUses);
        jdbcTemplate.update("""
                UPDATE deal_rooms
                SET invite_code = ?, invite_max_uses = ?, invite_used_count = 0
                WHERE id = ?
                """, code, maxUses, roomId);
    }

    public Optional<RoomInviteCode> lockInviteCodeByCode(String code) {
        return jdbcTemplate.query("""
                        SELECT id, room_id, code, invite_type, max_uses, used_count,
                               status, created_at, updated_at
                        FROM deal_room_invite_codes
                        WHERE code = ?
                        FOR UPDATE
                        """,
                (rs, rowNum) -> mapInviteCode(rs),
                code
        ).stream().findFirst();
    }

    public void recordInviteCodeUsage(RoomInviteCode inviteCode, Long tempUserId, Long memberId) {
        jdbcTemplate.update("""
                INSERT INTO deal_room_invite_code_usages (invite_code_id, room_id, temp_user_id, member_id)
                VALUES (?, ?, ?, ?)
                """, inviteCode.id(), inviteCode.roomId(), tempUserId, memberId);
        jdbcTemplate.update("""
                UPDATE deal_room_invite_codes
                SET used_count = used_count + 1
                WHERE id = ?
                """, inviteCode.id());
        jdbcTemplate.update("""
                UPDATE deal_rooms
                SET invite_used_count = invite_used_count + 1
                WHERE id = ? AND invite_code = ?
                """, inviteCode.roomId(), inviteCode.code());
    }

    public boolean inviteCodeExists(String code) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM deal_room_invite_codes
                WHERE code = ?
                """, Integer.class, code);
        return count != null && count > 0;
    }

    public List<RoomInviteCodeResponse> findInviteCodes(Long roomId) {
        return jdbcTemplate.query("""
                        SELECT id, code, invite_type, max_uses, used_count, status, created_at
                        FROM deal_room_invite_codes
                        WHERE room_id = ?
                        ORDER BY created_at DESC, id DESC
                        """,
                (rs, rowNum) -> new RoomInviteCodeResponse(
                        rs.getLong("id"),
                        rs.getString("code"),
                        rs.getString("invite_type"),
                        rs.getInt("max_uses"),
                        rs.getInt("used_count"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toInstant()
                ),
                roomId
        );
    }

    public Optional<RoomInviteCodeResponse> findPrimaryInviteCode(Long roomId) {
        return jdbcTemplate.query("""
                        SELECT id, code, invite_type, max_uses, used_count, status, created_at
                        FROM deal_room_invite_codes
                        WHERE room_id = ? AND status = 'ACTIVE'
                        ORDER BY invite_type = 'ROOM_SHARED' DESC, created_at ASC, id ASC
                        LIMIT 1
                        """,
                (rs, rowNum) -> new RoomInviteCodeResponse(
                        rs.getLong("id"),
                        rs.getString("code"),
                        rs.getString("invite_type"),
                        rs.getInt("max_uses"),
                        rs.getInt("used_count"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toInstant()
                ),
                roomId
        ).stream().findFirst();
    }

    public Optional<DealRoom> findRoomById(Long roomId) {
        return jdbcTemplate.query("""
                        SELECT id, name, host_temp_user_id, invite_code, invite_max_uses,
                               invite_used_count, status, created_at, updated_at
                        FROM deal_rooms
                        WHERE id = ?
                        """,
                (rs, rowNum) -> mapRoom(rs),
                roomId
        ).stream().findFirst();
    }

    public void updateRoomStatus(Long roomId, String status) {
        jdbcTemplate.update("""
                UPDATE deal_rooms
                SET status = ?
                WHERE id = ?
                """, status, roomId);
    }

    public Optional<DealRoom> findRoomByInviteCode(String inviteCode) {
        return jdbcTemplate.query("""
                        SELECT id, name, host_temp_user_id, invite_code, invite_max_uses,
                               invite_used_count, status, created_at, updated_at
                        FROM deal_rooms
                        WHERE invite_code = ?
                        UNION ALL
                        SELECT r.id, r.name, r.host_temp_user_id, r.invite_code, r.invite_max_uses,
                               r.invite_used_count, r.status, r.created_at, r.updated_at
                        FROM deal_rooms r
                        JOIN deal_room_invite_codes c ON c.room_id = r.id
                        WHERE c.code = ?
                        LIMIT 1
                        """,
                (rs, rowNum) -> mapRoom(rs),
                inviteCode,
                inviteCode
        ).stream().findFirst();
    }

    public Optional<RoomMember> findMember(Long roomId, Long tempUserId) {
        return jdbcTemplate.query("""
                        SELECT m.id, m.room_id, m.temp_user_id, m.member_no, m.role, u.username, m.joined_at
                        FROM deal_room_members m
                        JOIN temp_users u ON u.id = m.temp_user_id
                        WHERE m.room_id = ? AND m.temp_user_id = ?
                        """,
                (rs, rowNum) -> mapMember(rs),
                roomId,
                tempUserId
        ).stream().findFirst();
    }

    public Optional<RoomMember> findMemberById(Long roomId, Long memberId) {
        return jdbcTemplate.query("""
                        SELECT m.id, m.room_id, m.temp_user_id, m.member_no, m.role, u.username, m.joined_at
                        FROM deal_room_members m
                        JOIN temp_users u ON u.id = m.temp_user_id
                        WHERE m.room_id = ? AND m.id = ?
                        """,
                (rs, rowNum) -> mapMember(rs),
                roomId,
                memberId
        ).stream().findFirst();
    }

    public List<Long> findRoomTempUserIds(Long roomId) {
        return jdbcTemplate.queryForList("""
                SELECT temp_user_id
                FROM deal_room_members
                WHERE room_id = ?
                """, Long.class, roomId);
    }

    public RoomMember addMember(Long roomId, Long tempUserId, String role) {
        Optional<RoomMember> existing = findMember(roomId, tempUserId);
        if (existing.isPresent()) {
            return existing.get();
        }

        Integer memberNo = nextMemberNo(roomId);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO deal_room_members (room_id, temp_user_id, member_no, role)
                    VALUES (?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, roomId);
            statement.setLong(2, tempUserId);
            statement.setInt(3, memberNo);
            statement.setString(4, role);
            return statement;
        }, keyHolder);

        return findMember(roomId, tempUserId).orElseThrow();
    }

    public void refreshLegacyInviteUsage(Long roomId) {
        jdbcTemplate.update("""
                UPDATE deal_rooms r
                LEFT JOIN deal_room_invite_codes c ON c.room_id = r.id AND c.code = r.invite_code
                SET r.invite_max_uses = COALESCE(c.max_uses, r.invite_max_uses),
                    r.invite_used_count = COALESCE(c.used_count, r.invite_used_count)
                WHERE r.id = ?
                """, roomId);
    }

    public RoomItem createItem(Long roomId, Long memberId, String title, String description, String saleMode) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO deal_room_items (room_id, title, description, sale_mode, created_by_member_id)
                    VALUES (?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, roomId);
            statement.setString(2, title);
            statement.setString(3, description);
            statement.setString(4, saleMode);
            statement.setLong(5, memberId);
            return statement;
        }, keyHolder);

        Long id = keyHolder.getKey() == null ? null : keyHolder.getKey().longValue();
        Instant now = Instant.now();
        return new RoomItem(id, roomId, title, description, saleMode, true, memberId, now, now);
    }

    public Integer countItems(Long roomId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM deal_room_items
                WHERE room_id = ?
                """, Integer.class, roomId);
        return count == null ? 0 : count;
    }

    public Optional<RoomItem> findItem(Long roomId, Long itemId) {
        return jdbcTemplate.query("""
                        SELECT id, room_id, title, description, sale_mode, bidding_open,
                               created_by_member_id, created_at, updated_at
                        FROM deal_room_items
                        WHERE room_id = ? AND id = ?
                        """,
                (rs, rowNum) -> mapItem(rs),
                roomId,
                itemId
        ).stream().findFirst();
    }

    public void closeBidding(Long roomId, Long itemId) {
        jdbcTemplate.update("""
                UPDATE deal_room_items
                SET bidding_open = 0
                WHERE room_id = ? AND id = ?
                """, roomId, itemId);
    }

    public void createBid(Long roomId, Long itemId, Long memberId, BigDecimal amount) {
        jdbcTemplate.update("""
                INSERT INTO deal_room_bids (room_id, item_id, member_id, amount)
                VALUES (?, ?, ?, ?)
                """, roomId, itemId, memberId, amount);
    }

    public void removeMember(Long roomId, Long memberId) {
        jdbcTemplate.update("""
                DELETE FROM deal_room_bids
                WHERE room_id = ? AND member_id = ?
                """, roomId, memberId);
        jdbcTemplate.update("""
                UPDATE deal_room_invite_codes c
                JOIN deal_room_invite_code_usages u ON u.invite_code_id = c.id
                SET c.used_count = GREATEST(c.used_count - 1, 0)
                WHERE u.room_id = ? AND u.member_id = ?
                """, roomId, memberId);
        jdbcTemplate.update("""
                DELETE FROM deal_room_invite_code_usages
                WHERE room_id = ? AND member_id = ?
                """, roomId, memberId);
        jdbcTemplate.update("""
                DELETE FROM deal_room_members
                WHERE room_id = ? AND id = ?
                """, roomId, memberId);
    }

    public void clearRoom(Long roomId) {
        jdbcTemplate.update("""
                DELETE FROM deal_room_bids
                WHERE room_id = ?
                """, roomId);
        jdbcTemplate.update("""
                DELETE FROM deal_room_invite_code_usages
                WHERE room_id = ?
                """, roomId);
        jdbcTemplate.update("""
                DELETE FROM deal_room_items
                WHERE room_id = ?
                """, roomId);
        jdbcTemplate.update("""
                DELETE FROM deal_room_invite_codes
                WHERE room_id = ?
                """, roomId);
        jdbcTemplate.update("""
                DELETE FROM deal_room_members
                WHERE room_id = ?
                """, roomId);
        jdbcTemplate.update("""
                DELETE FROM deal_rooms
                WHERE id = ?
                """, roomId);
    }

    public void deleteUnusedTempUsers(List<Long> tempUserIds) {
        for (Long tempUserId : tempUserIds) {
            jdbcTemplate.update("""
                    DELETE FROM temp_users
                    WHERE id = ?
                      AND NOT EXISTS (
                          SELECT 1
                          FROM deal_room_members
                          WHERE temp_user_id = ?
                      )
                      AND NOT EXISTS (
                          SELECT 1
                          FROM deal_rooms
                          WHERE host_temp_user_id = ?
                      )
                      AND NOT EXISTS (
                          SELECT 1
                          FROM deal_room_invite_code_usages
                          WHERE temp_user_id = ?
                      )
                    """, tempUserId, tempUserId, tempUserId, tempUserId);
        }
    }

    public List<RoomMemberResponse> findMembers(Long roomId) {
        return jdbcTemplate.query("""
                        SELECT m.id, m.temp_user_id, m.member_no, m.role, u.username, m.joined_at
                        FROM deal_room_members m
                        JOIN temp_users u ON u.id = m.temp_user_id
                        WHERE m.room_id = ?
                        ORDER BY m.member_no
                        """,
                (rs, rowNum) -> new RoomMemberResponse(
                        rs.getLong("id"),
                        rs.getLong("temp_user_id"),
                        rs.getInt("member_no"),
                        rs.getString("role"),
                        rs.getString("username"),
                        rs.getTimestamp("joined_at").toInstant()
                ),
                roomId
        );
    }

    public List<RoomItemResponse> findItems(Long roomId) {
        List<RoomItem> items = jdbcTemplate.query("""
                        SELECT id, room_id, title, description, sale_mode, bidding_open,
                               created_by_member_id, created_at, updated_at
                        FROM deal_room_items
                        WHERE room_id = ?
                        ORDER BY created_at DESC, id DESC
                        """,
                (rs, rowNum) -> mapItem(rs),
                roomId
        );

        return items.stream()
                .map(item -> {
                    boolean hiddenTender = "TENDER".equals(item.saleMode()) && Boolean.TRUE.equals(item.biddingOpen());
                    List<RoomBidResponse> bids = hiddenTender ? List.of() : findBids(item.id());
                    RoomBidResponse topBid = bids.isEmpty() ? null : bids.get(0);
                    return new RoomItemResponse(
                            item.id(),
                            item.title(),
                            item.description(),
                            item.saleMode(),
                            item.biddingOpen(),
                            topBid,
                            bids,
                            item.createdAt()
                    );
                })
                .toList();
    }

    private Integer nextMemberNo(Long roomId) {
        Integer next = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(member_no), 0) + 1
                FROM deal_room_members
                WHERE room_id = ?
                """, Integer.class, roomId);
        return next == null ? 1 : next;
    }

    private List<RoomBidResponse> findBids(Long itemId) {
        return jdbcTemplate.query("""
                        SELECT b.id, b.member_id, m.member_no, u.username, b.amount, b.created_at
                        FROM deal_room_bids b
                        JOIN deal_room_members m ON m.id = b.member_id
                        JOIN temp_users u ON u.id = m.temp_user_id
                        WHERE b.item_id = ?
                        ORDER BY b.amount DESC, b.created_at ASC, b.id ASC
                        """,
                (rs, rowNum) -> new RoomBidResponse(
                        rs.getLong("id"),
                        rs.getLong("member_id"),
                        rs.getInt("member_no"),
                        rs.getString("username"),
                        rs.getBigDecimal("amount"),
                        rs.getTimestamp("created_at").toInstant()
                ),
                itemId
        );
    }

    private DealRoom mapRoom(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new DealRoom(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getLong("host_temp_user_id"),
                rs.getString("invite_code"),
                rs.getInt("invite_max_uses"),
                rs.getInt("invite_used_count"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        );
    }

    private RoomInviteCode mapInviteCode(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new RoomInviteCode(
                rs.getLong("id"),
                rs.getLong("room_id"),
                rs.getString("code"),
                rs.getString("invite_type"),
                rs.getInt("max_uses"),
                rs.getInt("used_count"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        );
    }

    private RoomMember mapMember(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new RoomMember(
                rs.getLong("id"),
                rs.getLong("room_id"),
                rs.getLong("temp_user_id"),
                rs.getInt("member_no"),
                rs.getString("role"),
                rs.getString("username"),
                rs.getTimestamp("joined_at").toInstant()
        );
    }

    private RoomItem mapItem(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new RoomItem(
                rs.getLong("id"),
                rs.getLong("room_id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("sale_mode"),
                rs.getBoolean("bidding_open"),
                rs.getLong("created_by_member_id"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        );
    }
}
