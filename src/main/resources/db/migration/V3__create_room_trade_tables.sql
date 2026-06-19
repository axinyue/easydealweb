CREATE TABLE deal_rooms (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '房间ID',
    name VARCHAR(80) NOT NULL COMMENT '房间名称',
    host_temp_user_id BIGINT NOT NULL COMMENT '房主临时用户ID',
    invite_code VARCHAR(24) NOT NULL COMMENT '房间邀请码',
    invite_max_uses INT NOT NULL DEFAULT 50 COMMENT '邀请码最多可使用次数',
    invite_used_count INT NOT NULL DEFAULT 0 COMMENT '邀请码已使用次数',
    status VARCHAR(24) NOT NULL DEFAULT 'OPEN' COMMENT '房间状态',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_deal_rooms_invite_code (invite_code),
    KEY idx_deal_rooms_host_temp_user_id (host_temp_user_id),
    CONSTRAINT fk_deal_rooms_host_temp_user_id FOREIGN KEY (host_temp_user_id) REFERENCES temp_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易房间表';

CREATE TABLE deal_room_members (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '房间成员ID',
    room_id BIGINT NOT NULL COMMENT '房间ID',
    temp_user_id BIGINT NOT NULL COMMENT '临时用户ID',
    member_no INT NOT NULL COMMENT '房间内成员编号',
    role VARCHAR(24) NOT NULL COMMENT '成员角色',
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_deal_room_members_room_user (room_id, temp_user_id),
    UNIQUE KEY uk_deal_room_members_room_no (room_id, member_no),
    KEY idx_deal_room_members_temp_user_id (temp_user_id),
    CONSTRAINT fk_deal_room_members_room_id FOREIGN KEY (room_id) REFERENCES deal_rooms (id),
    CONSTRAINT fk_deal_room_members_temp_user_id FOREIGN KEY (temp_user_id) REFERENCES temp_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='房间成员表';

CREATE TABLE deal_room_items (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '房间商品ID',
    room_id BIGINT NOT NULL COMMENT '房间ID',
    title VARCHAR(120) NOT NULL COMMENT '商品名称',
    description VARCHAR(500) NULL COMMENT '商品补充说明',
    sale_mode VARCHAR(24) NOT NULL DEFAULT 'AUCTION' COMMENT '出售方式',
    bidding_open TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否允许出价',
    created_by_member_id BIGINT NOT NULL COMMENT '创建商品的成员ID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_deal_room_items_room_id (room_id),
    CONSTRAINT fk_deal_room_items_room_id FOREIGN KEY (room_id) REFERENCES deal_rooms (id),
    CONSTRAINT fk_deal_room_items_created_by_member_id FOREIGN KEY (created_by_member_id) REFERENCES deal_room_members (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='房间商品表';

CREATE TABLE deal_room_bids (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '出价ID',
    room_id BIGINT NOT NULL COMMENT '房间ID',
    item_id BIGINT NOT NULL COMMENT '商品ID',
    member_id BIGINT NOT NULL COMMENT '出价成员ID',
    amount DECIMAL(12,2) NOT NULL COMMENT '出价金额',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '出价时间',
    PRIMARY KEY (id),
    KEY idx_deal_room_bids_item_amount (item_id, amount, created_at),
    KEY idx_deal_room_bids_room_id (room_id),
    CONSTRAINT fk_deal_room_bids_room_id FOREIGN KEY (room_id) REFERENCES deal_rooms (id),
    CONSTRAINT fk_deal_room_bids_item_id FOREIGN KEY (item_id) REFERENCES deal_room_items (id),
    CONSTRAINT fk_deal_room_bids_member_id FOREIGN KEY (member_id) REFERENCES deal_room_members (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='房间商品出价表';
