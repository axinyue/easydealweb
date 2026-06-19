CREATE TABLE deal_room_invite_codes (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '邀请码ID',
    room_id BIGINT NOT NULL COMMENT '房间ID',
    code VARCHAR(24) NOT NULL COMMENT '邀请码',
    invite_type VARCHAR(24) NOT NULL COMMENT '邀请码类型：ONE_PERSON一码一人，ROOM_SHARED一码一房，MULTI_PERSON一码多人',
    max_uses INT NOT NULL COMMENT '最多可使用次数',
    used_count INT NOT NULL DEFAULT 0 COMMENT '已使用次数',
    status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE' COMMENT '邀请码状态：ACTIVE有效，DISABLED停用',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_deal_room_invite_codes_code (code),
    KEY idx_deal_room_invite_codes_room_id (room_id),
    CONSTRAINT fk_deal_room_invite_codes_room_id FOREIGN KEY (room_id) REFERENCES deal_rooms (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='房间邀请码表';

CREATE TABLE deal_room_invite_code_usages (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '邀请码使用记录ID',
    invite_code_id BIGINT NOT NULL COMMENT '邀请码ID',
    room_id BIGINT NOT NULL COMMENT '房间ID',
    temp_user_id BIGINT NOT NULL COMMENT '使用邀请码的临时用户ID',
    member_id BIGINT NOT NULL COMMENT '加入后的房间成员ID',
    used_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '使用时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_deal_room_invite_code_usages_code_user (invite_code_id, temp_user_id),
    KEY idx_deal_room_invite_code_usages_room_id (room_id),
    KEY idx_deal_room_invite_code_usages_temp_user_id (temp_user_id),
    CONSTRAINT fk_deal_room_invite_code_usages_code_id FOREIGN KEY (invite_code_id) REFERENCES deal_room_invite_codes (id),
    CONSTRAINT fk_deal_room_invite_code_usages_room_id FOREIGN KEY (room_id) REFERENCES deal_rooms (id),
    CONSTRAINT fk_deal_room_invite_code_usages_temp_user_id FOREIGN KEY (temp_user_id) REFERENCES temp_users (id),
    CONSTRAINT fk_deal_room_invite_code_usages_member_id FOREIGN KEY (member_id) REFERENCES deal_room_members (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='房间邀请码使用记录表';

INSERT INTO deal_room_invite_codes (room_id, code, invite_type, max_uses, used_count, status, created_at, updated_at)
SELECT id, invite_code, 'ROOM_SHARED', invite_max_uses, invite_used_count, 'ACTIVE', created_at, updated_at
FROM deal_rooms;
