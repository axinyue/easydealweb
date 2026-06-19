CREATE TABLE temp_users (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '临时用户ID',
    token_hash CHAR(64) NOT NULL COMMENT '临时访问令牌哈希',
    username VARCHAR(64) NOT NULL COMMENT '临时用户名',
    locale VARCHAR(16) NULL COMMENT '用户名语言标识',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_temp_users_token_hash (token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='临时用户表';
