DROP DATABASE IF EXISTS raffle_db;
CREATE DATABASE raffle_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE raffle_db;

DROP TABLE IF EXISTS raffle_record;
DROP TABLE IF EXISTS raffle_refund;
DROP TABLE IF EXISTS user_refund_quota;
DROP TABLE IF EXISTS award_task;
DROP TABLE IF EXISTS award_received;
DROP TABLE IF EXISTS strategy_rule;
DROP TABLE IF EXISTS strategy_award;
DROP TABLE IF EXISTS strategy;
DROP TABLE IF EXISTS activity;
DROP TABLE IF EXISTS award;

CREATE TABLE award (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    award_id BIGINT NOT NULL COMMENT '奖品ID',
    award_type TINYINT NOT NULL COMMENT '奖品类型(1:实物, 2:虚拟)',
    award_name VARCHAR(128) NOT NULL COMMENT '奖品名称',
    award_value VARCHAR(255) DEFAULT NULL COMMENT '奖品价值/配置值',
    award_desc VARCHAR(255) DEFAULT NULL COMMENT '奖品描述',
    PRIMARY KEY (id),
    UNIQUE KEY uk_award_id (award_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='奖品表';

CREATE TABLE strategy (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    strategy_id BIGINT NOT NULL COMMENT '策略ID',
    strategy_desc VARCHAR(255) NOT NULL COMMENT '策略描述',
    rule_models VARCHAR(255) DEFAULT NULL COMMENT '前置规则模型列表',
    PRIMARY KEY (id),
    UNIQUE KEY uk_strategy_id (strategy_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='策略表';

CREATE TABLE activity (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    activity_id BIGINT NOT NULL COMMENT '活动ID',
    activity_name VARCHAR(128) NOT NULL COMMENT '活动名称',
    activity_desc VARCHAR(255) DEFAULT NULL COMMENT '活动描述',
    strategy_id BIGINT NOT NULL COMMENT '绑定策略ID',
    page_title VARCHAR(128) NOT NULL COMMENT '页面标题',
    page_subtitle VARCHAR(255) DEFAULT NULL COMMENT '页面副标题',
    banner_url VARCHAR(255) DEFAULT NULL COMMENT '页面横幅',
    theme_color VARCHAR(32) DEFAULT NULL COMMENT '主题颜色',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态(1:启用,0:停用)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_activity_id (activity_id),
    KEY idx_activity_strategy (strategy_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动表';

CREATE TABLE strategy_award (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    strategy_id BIGINT NOT NULL COMMENT '策略ID',
    award_id BIGINT NOT NULL COMMENT '奖品ID',
    rule_models VARCHAR(255) DEFAULT NULL COMMENT '后置规则模型列表',
    award_title VARCHAR(128) NOT NULL COMMENT '奖品标题',
    award_allocate INT NOT NULL DEFAULT 0 COMMENT '分配总量',
    award_surplus INT NOT NULL DEFAULT 0 COMMENT '剩余库存',
    award_rate DECIMAL(10,4) NOT NULL DEFAULT 0.0000 COMMENT '中奖概率',
    award_index INT NOT NULL DEFAULT 0 COMMENT '奖品排序索引',
    PRIMARY KEY (id),
    UNIQUE KEY uk_strategy_award (strategy_id, award_id),
    KEY idx_strategy_award_strategy (strategy_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='策略奖品表';

CREATE TABLE strategy_rule (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    strategy_id BIGINT NOT NULL COMMENT '策略ID',
    award_id BIGINT DEFAULT NULL COMMENT '奖品ID',
    rule_model VARCHAR(64) NOT NULL COMMENT '规则模型',
    rule_value VARCHAR(255) DEFAULT NULL COMMENT '规则值',
    rule_desc VARCHAR(255) DEFAULT NULL COMMENT '规则描述',
    PRIMARY KEY (id),
    KEY idx_strategy_rule_strategy (strategy_id),
    KEY idx_strategy_rule_award (award_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='策略规则表';

CREATE TABLE raffle_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    strategy_id BIGINT NOT NULL COMMENT '策略ID',
    award_id BIGINT NOT NULL COMMENT '奖品ID',
    award_name VARCHAR(128) NOT NULL COMMENT '奖品名称',
    success TINYINT(1) NOT NULL COMMENT '是否中奖',
    message VARCHAR(255) DEFAULT NULL COMMENT '结果信息',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_raffle_record_user_strategy (user_id, strategy_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='中奖记录表';

CREATE TABLE award_task (
    task_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    strategy_id BIGINT NOT NULL COMMENT '策略ID',
    award_id BIGINT NOT NULL COMMENT '奖品ID',
    award_name VARCHAR(128) NOT NULL COMMENT '奖品名称',
    task_status VARCHAR(32) NOT NULL COMMENT '任务状态(PENDING/PROCESSING/AWARDED/REFUNDED/FAILED)',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '失败重试次数',
    fail_reason VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (task_id),
    UNIQUE KEY uk_award_task_id (task_id),
    KEY idx_award_task_user_strategy (user_id, strategy_id),
    KEY idx_award_task_status_time (task_status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发奖任务表';

CREATE TABLE award_received (
    received_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    strategy_id BIGINT NOT NULL COMMENT '策略ID',
    award_id BIGINT NOT NULL COMMENT '奖品ID',
    award_name VARCHAR(128) NOT NULL COMMENT '奖品名称',
    receive_status VARCHAR(32) NOT NULL COMMENT '领奖状态(RECEIVED)',
    receive_time DATETIME NOT NULL COMMENT '领奖时间',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (received_id),
    UNIQUE KEY uk_award_received_task_id (task_id),
    KEY idx_award_received_user_strategy (user_id, strategy_id),
    KEY idx_award_received_status_time (receive_status, receive_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户获奖表';

CREATE TABLE user_refund_quota (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    strategy_id BIGINT NOT NULL COMMENT '策略ID',
    used_count INT NOT NULL DEFAULT 0 COMMENT '已用退款次数',
    max_count INT NOT NULL DEFAULT 3 COMMENT '最大退款次数',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (user_id, strategy_id),
    KEY idx_refund_quota_strategy (strategy_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户退款配额表';

CREATE TABLE raffle_refund (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    refund_id VARCHAR(64) NOT NULL COMMENT '退款请求幂等号',
    task_id BIGINT NOT NULL COMMENT '发奖任务ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    strategy_id BIGINT NOT NULL COMMENT '策略ID',
    award_id BIGINT NOT NULL COMMENT '奖品ID',
    refund_status VARCHAR(32) NOT NULL COMMENT '退款状态(REQUESTED/REFUNDED/REJECTED)',
    refund_message VARCHAR(255) DEFAULT NULL COMMENT '退款说明',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_raffle_refund_refund_id (refund_id),
    UNIQUE KEY uk_raffle_refund_task_id (task_id),
    KEY idx_raffle_refund_user_strategy (user_id, strategy_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款申请表';

INSERT INTO award(award_id, award_type, award_name, award_value, award_desc) VALUES
(101, 2, '谢谢惠顾', '0', '兜底奖品'),
(201, 2, '五元优惠券', '5', '小额优惠券'),
(202, 1, '充电宝', '1', '实物奖品'),
(203, 1, '笔记本电脑', '1', '大奖');

INSERT INTO strategy(strategy_id, strategy_desc, rule_models) VALUES
(1001, '默认抽奖策略', 'rule_blacklist,rule_weight'),
(1002, '加权抽奖策略', '');

INSERT INTO activity(activity_id, activity_name, activity_desc, strategy_id, page_title, page_subtitle, banner_url, theme_color, sort_no, status) VALUES
(20001, '618 大促', '618 主题抽奖活动', 1001, '618 抽奖', '天天抽大奖', '/images/activity-618.png', '#ff6a00', 1, 1),
(20002, '周年庆', '周年庆主题抽奖活动', 1002, '周年庆抽奖', '参与就有机会', '/images/activity-anniversary.png', '#1f8ef1', 2, 1);

INSERT INTO strategy_award(strategy_id, award_id, rule_models, award_title, award_allocate, award_surplus, award_rate, award_index) VALUES
(1001, 101, '', '谢谢惠顾', 0, 0, 0.0000, 0),
(1001, 201, 'rule_stock', '五元优惠券', 100, 10, 0.5500, 1),
(1001, 202, 'rule_stock', '充电宝', 20, 2, 0.3500, 2),
(1001, 203, 'rule_lock,rule_stock', '笔记本电脑', 5, 1, 0.1000, 3),
(1002, 101, '', '谢谢惠顾', 0, 0, 0.0000, 0),
(1002, 201, 'rule_stock', '五元优惠券', 100, 10, 0.3000, 1),
(1002, 202, 'rule_stock', '充电宝', 20, 2, 0.4000, 2),
(1002, 203, 'rule_lock,rule_stock', '笔记本电脑', 5, 1, 0.3000, 3);

INSERT INTO strategy_rule(strategy_id, award_id, rule_model, rule_value, rule_desc) VALUES
(1001, NULL, 'rule_blacklist', '9001,9002', '黑名单命中直接兜底'),
(1001, NULL, 'rule_weight', '1003,1004;1002', '加权用户切换到策略1002'),
(1001, 201, 'rule_stock', 'true', '五元优惠券库存扣减'),
(1001, 202, 'rule_stock', 'true', '充电宝库存扣减'),
(1001, 203, 'rule_lock', '3', '用户抽奖次数达到3次才可中大奖'),
(1001, 203, 'rule_stock', 'true', '笔记本库存扣减'),
(1002, 201, 'rule_stock', 'true', '五元优惠券库存扣减'),
(1002, 202, 'rule_stock', 'true', '充电宝库存扣减'),
(1002, 203, 'rule_lock', '1', '首次抽奖可中大奖'),
(1002, 203, 'rule_stock', 'true', '笔记本库存扣减');

INSERT INTO raffle_record(user_id, strategy_id, award_id, award_name, success, message, create_time) VALUES
(1003, 1001, 201, '五元优惠券', 1, 'OK', NOW()),
(1004, 1001, 101, '谢谢惠顾', 0, 'user is in blacklist, fallback to consolation prize', NOW());
