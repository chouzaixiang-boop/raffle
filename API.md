# 接口文档

## 运行前准备

1. 导入 [src/main/resources/sql/raffle_init.sql](src/main/resources/sql/raffle_init.sql) 到 MySQL。
2. 启动 MySQL 和 Redis。
3. 根据环境变量调整 `application.properties` 中的默认连接信息。

## 抽奖接口

### `POST /api/raffle/draw`

请求体：

```json
{
  "userId": 1001,
  "strategyId": 1001
}
```

返回体：

```json
{
  "userId": 1001,
  "strategyId": 1001,
  "awardId": 201,
  "awardName": "五元优惠券",
  "success": true,
  "message": "OK"
}
```

### 字段说明

- `userId`：用户 ID，必填。
- `strategyId`：策略 ID，必填。
- `awardId`：最终抽中的奖品 ID。
- `awardName`：奖品名称。
- `success`：是否最终中奖。
- `message`：结果说明，命中黑名单、库存不足或次数不足时会给出原因。

## 示例调用

```bash
curl -X POST http://localhost:8080/api/raffle/draw \
  -H "Content-Type: application/json" \
  -d "{\"userId\":1001,\"strategyId\":1001}"
```

## 数据初始化说明

SQL 文件里已经包含：

- `award` 奖品基础数据
- `strategy` 策略数据
- `strategy_award` 策略奖品配置
- `strategy_rule` 前置与后置规则
- `raffle_record` 示例中奖记录

Redis 不通过 SQL 初始化，而是在应用启动时根据 `strategy_award.award_surplus` 自动预热库存键。
