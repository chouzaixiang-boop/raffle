DROP DATABASE IF EXISTS raffle_db;
CREATE DATABASE raffle_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE raffle_db;

DROP TABLE IF EXISTS raffle_record;
DROP TABLE IF EXISTS strategy_rule;
DROP TABLE IF EXISTS strategy_award;
DROP TABLE IF EXISTS strategy;
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

INSERT INTO award(award_id, award_type, award_name, award_value, award_desc) VALUES
(101, 2, '谢谢惠顾', '0', '兜底奖品'),
(201, 2, '五元优惠券', '5', '小额优惠券'),
(202, 1, '充电宝', '1', '实物奖品'),
(203, 1, '笔记本电脑', '1', '大奖');

INSERT INTO strategy(strategy_id, strategy_desc, rule_models) VALUES
(1001, '默认抽奖策略', 'rule_blacklist,rule_weight'),
(1002, '加权抽奖策略', '');

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
