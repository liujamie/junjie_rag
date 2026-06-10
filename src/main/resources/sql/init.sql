-- MySQL 5.7 版本适配

-- ----------------------------
-- Table structure for vector_store
-- ----------------------------
DROP TABLE IF EXISTS `vector_store`;
CREATE TABLE `vector_store` (
                                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                `content` TEXT,
                                `metadata` JSON,
                                `embedding` JSON COMMENT '向量数据，存储为JSON数组，原PostgreSQL为vector(1536)',
                                PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ----------------------------
-- Table structure for tb_user
-- ----------------------------
DROP TABLE IF EXISTS `tb_user`;
CREATE TABLE `tb_user` (
                           `id` INT NOT NULL AUTO_INCREMENT,
                           `name` VARCHAR(255) NOT NULL COMMENT '姓名',
                           `user_name` VARCHAR(255) NOT NULL COMMENT '用户名',
                           `password` VARCHAR(255) NOT NULL COMMENT '密码',
                           `phone` VARCHAR(255) NOT NULL COMMENT '手机号',
                           `sex` VARCHAR(255) NOT NULL COMMENT '性别',
                           `id_number` VARCHAR(255) NOT NULL COMMENT '身份证号',
                           `status` INT NOT NULL DEFAULT 1 COMMENT '状态 0：禁用 1：启用',
                           `create_time` DATE COMMENT '创建时间',
                           `update_time` DATE COMMENT '更新时间',
                           `create_user` BIGINT COMMENT '创建人',
                           `update_user` BIGINT COMMENT '修改人',
                           PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=666498 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ----------------------------
-- Records of tb_user
-- ----------------------------
INSERT INTO `tb_user` (`id`, `name`, `user_name`, `password`, `phone`, `sex`, `id_number`, `status`, `create_time`, `update_time`, `create_user`, `update_user`)
VALUES (666497, '管理员', 'admin', '21232f297a57a5a743894a0e4a801fc3', '13800138000', '男', '11010519491231002X', 1, '2025-03-03', '2025-03-03', NULL, NULL);


-- ----------------------------
-- Table structure for ali_oss_file
-- ----------------------------
DROP TABLE IF EXISTS `ali_oss_file`;
CREATE TABLE `ali_oss_file` (
                                `id` BIGINT NOT NULL AUTO_INCREMENT,
                                `file_name` VARCHAR(255) COMMENT '文件名',
                                `url` VARCHAR(500) COMMENT '链接地址',
                                `vector_id` TEXT COMMENT '该文件分割出的多段向量文本ID',
                                `create_time` TIMESTAMP NULL DEFAULT NULL COMMENT '创建时间',
                                `update_time` TIMESTAMP NULL DEFAULT NULL COMMENT '更新时间',
                                PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='阿里云OSS文件表';


-- ----------------------------
-- Table structure for log_info
-- ----------------------------
-- 如果已存在旧表（升级），执行以下迁移语句:
-- ALTER TABLE log_info ADD COLUMN trace_id VARCHAR(32) COMMENT '链路追踪ID' AFTER id;
-- ALTER TABLE log_info ADD COLUMN user_id BIGINT COMMENT '操作用户ID' AFTER trace_id;
-- ALTER TABLE log_info MODIFY COLUMN request_time DATETIME(3) COMMENT '请求时间戳';
-- ALTER TABLE log_info ADD COLUMN duration BIGINT COMMENT '执行耗时(毫秒)' AFTER response;
-- ALTER TABLE log_info ADD COLUMN status VARCHAR(16) DEFAULT 'success' COMMENT '状态: success/fail' AFTER duration;
-- ALTER TABLE log_info ADD COLUMN error_message TEXT COMMENT '异常信息' AFTER status;
-- ALTER TABLE log_info ADD INDEX idx_trace_id (trace_id);
-- ALTER TABLE log_info ADD INDEX idx_user_id (user_id);
-- ALTER TABLE log_info ADD INDEX idx_request_time (request_time);
DROP TABLE IF EXISTS `log_info`;
CREATE TABLE `log_info` (
                            `id` BIGINT NOT NULL AUTO_INCREMENT,
                            `trace_id` VARCHAR(32) COMMENT '链路追踪ID',
                            `user_id` BIGINT COMMENT '操作用户ID',
                            `method_name` VARCHAR(255) COMMENT '方法名',
                            `class_name` VARCHAR(255) COMMENT '类目',
                            `request_time` DATETIME(3) COMMENT '请求时间戳',
                            `request_params` TEXT COMMENT '请求参数',
                            `response` TEXT COMMENT '响应结果',
                            `duration` BIGINT COMMENT '执行耗时(毫秒)',
                            `status` VARCHAR(16) DEFAULT 'success' COMMENT '状态: success/fail',
                            `error_message` TEXT COMMENT '异常信息',
                            PRIMARY KEY (`id`),
                            INDEX idx_trace_id (`trace_id`),
                            INDEX idx_user_id (`user_id`),
                            INDEX idx_request_time (`request_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日志信息表';


-- ----------------------------
-- Table structure for sensitive_word
-- ----------------------------
DROP TABLE IF EXISTS `sensitive_word`;
CREATE TABLE `sensitive_word` (
                                  `id` INT NOT NULL AUTO_INCREMENT,
                                  `word` VARCHAR(255) COMMENT '敏感词内容',
                                  `category` VARCHAR(255) COMMENT '敏感词类别',
                                  `status` VARCHAR(50) COMMENT '敏感词状态',
                                  `created_at` VARCHAR(50) COMMENT '创建时间戳',
                                  `updated_at` VARCHAR(50) COMMENT '更新时间戳',
                                  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='敏感词表';


-- ----------------------------
-- Table structure for word_frequency
-- ----------------------------
DROP TABLE IF EXISTS `word_frequency`;
CREATE TABLE `word_frequency` (
                                  `id` INT NOT NULL AUTO_INCREMENT,
                                  `word` VARCHAR(255) COMMENT '分词',
                                  `count_num` INT COMMENT '出现频次',
                                  `business_type` VARCHAR(255) COMMENT '业务类型',
                                  `create_time` DATE COMMENT '创建时间',
                                  `update_time` DATE COMMENT '更新时间',
                                  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='词频统计表';


-- ----------------------------
-- Table structure for sensitive_category
-- ----------------------------
DROP TABLE IF EXISTS `sensitive_category`;
CREATE TABLE `sensitive_category` (
                                      `id` INT NOT NULL AUTO_INCREMENT,
                                      `category_name` VARCHAR(255) COMMENT '分类名',
                                      `created_time` DATE COMMENT '创建时间',
                                      `update_time` DATE COMMENT '更新时间',
                                      `status` VARCHAR(50) COMMENT '状态',
                                      PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='敏感词分类表';

-- ----------------------------
-- Table structure for llm_call_record
-- ----------------------------
DROP TABLE IF EXISTS `llm_call_record`;
CREATE TABLE `llm_call_record` (
                                   `id` BIGINT NOT NULL AUTO_INCREMENT,
                                   `trace_id` VARCHAR(32) COMMENT '链路追踪ID',
                                   `user_id` BIGINT COMMENT '操作用户ID',
                                   `service_name` VARCHAR(32) COMMENT '服务名: rewrite/rerank/searchWeb/chat/rag',
                                   `model_name` VARCHAR(64) COMMENT '模型名',
                                   `input_tokens` INT DEFAULT 0 COMMENT '输入token数(估算)',
                                   `output_tokens` INT DEFAULT 0 COMMENT '输出token数(估算)',
                                   `duration_ms` BIGINT COMMENT '耗时(毫秒)',
                                   `status` VARCHAR(16) DEFAULT 'success' COMMENT '状态: success/fail',
                                   `request_preview` VARCHAR(500) COMMENT '请求摘要',
                                   `response_preview` VARCHAR(500) COMMENT '响应摘要',
                                   `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
                                   PRIMARY KEY (`id`),
                                   INDEX idx_llm_trace_id (`trace_id`),
                                   INDEX idx_llm_user_id (`user_id`),
                                   INDEX idx_llm_service (`service_name`),
                                   INDEX idx_llm_create_time (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='LLM调用记录表';

-- ----------------------------
-- Table structure for llm_model_config
-- ----------------------------
DROP TABLE IF EXISTS `llm_model_config`;
CREATE TABLE `llm_model_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(64) NOT NULL COMMENT '显示名称',
    `provider` VARCHAR(32) NOT NULL COMMENT '提供商: deepseek/openai/ollama',
    `base_url` VARCHAR(256) COMMENT 'API 地址',
    `model_name` VARCHAR(64) COMMENT '模型名',
    `api_key` VARCHAR(256) COMMENT 'API Key',
    `temperature` DOUBLE DEFAULT 0.7 COMMENT '温度',
    `max_tokens` INT DEFAULT 4096 COMMENT '最大输出token',
    `top_p` DOUBLE DEFAULT 0.9 COMMENT 'Top P',
    `is_default` TINYINT DEFAULT 0 COMMENT '是否预置',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='LLM模型配置表';

-- 会话历史存储表
-- 在 MySQL 中执行
DROP TABLE IF EXISTS `conversation`;
CREATE TABLE IF NOT EXISTS conversation (
                                            id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '会话ID',
                                            user_id     BIGINT       NOT NULL COMMENT '用户ID, 关联 tb_user.id',
                                            title       VARCHAR(100) DEFAULT '新对话' COMMENT '会话标题',
    status      TINYINT      DEFAULT 1 COMMENT '状态: 1-正常 0-删除',
    created_time DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id)
    ) COMMENT '会话表';

DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE IF NOT EXISTS chat_message (
                                            id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
                                            conversation_id BIGINT       NOT NULL COMMENT '会话ID',
                                            role            VARCHAR(20)  NOT NULL COMMENT '角色: user/assistant',
    content         TEXT         COMMENT '消息内容',
    created_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_conversation_id (conversation_id)
    ) COMMENT '聊天消息表';
