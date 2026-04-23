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

## 奖品装配接口（补库存）

### `POST /api/raffle/assemble/stock`

请求体：

```json
{
  "strategyId": 1001,
  "awardId": 201,
  "replenishCount": 5
}
```

返回体示例：

```json
{
  "strategyId": 1001,
  "awardId": 201,
  "replenishCount": 5,
  "dbBefore": 0,
  "redisBefore": 0,
  "afterStock": 5,
  "message": "assembled and synchronized"
}
```

### 测试数据建议

- 用例 1：`{ "strategyId": 1001, "awardId": 201, "replenishCount": 5 }`
- 用例 2：`{ "strategyId": 1001, "awardId": 202, "replenishCount": 20 }`
- 用例 3：`{ "strategyId": 1002, "awardId": 203, "replenishCount": 1 }`

### 示例调用

```bash
curl -X POST http://localhost:8080/api/raffle/assemble/stock \
  -H "Content-Type: application/json" \
  -d "{\"strategyId\":1001,\"awardId\":201,\"replenishCount\":5}"
```

## 批量奖品装配接口（一次补多个奖品）

### `POST /api/raffle/assemble/stock/batch`

请求体：

```json
{
  "items": [
    { "strategyId": 1001, "awardId": 201, "replenishCount": 20 },
    { "strategyId": 1001, "awardId": 202, "replenishCount": 10 },
    { "strategyId": 1001, "awardId": 203, "replenishCount": 2 },
    { "strategyId": 1002, "awardId": 201, "replenishCount": 20 },
    { "strategyId": 1002, "awardId": 202, "replenishCount": 10 },
    { "strategyId": 1002, "awardId": 203, "replenishCount": 2 }
  ]
}
```

返回体示例：

```json
{
  "total": 6,
  "successCount": 6,
  "failedCount": 0,
  "results": [
    {
      "strategyId": 1001,
      "awardId": 201,
      "replenishCount": 20,
      "success": true,
      "dbBefore": 0,
      "redisBefore": 0,
      "afterStock": 20,
      "message": "assembled and synchronized"
    }
  ]
}
```

### 一次性装配全部奖品示例

```bash
curl -X POST http://localhost:8080/api/raffle/assemble/stock/batch \
  -H "Content-Type: application/json" \
  -d "{\"items\":[{\"strategyId\":1001,\"awardId\":201,\"replenishCount\":20},{\"strategyId\":1001,\"awardId\":202,\"replenishCount\":10},{\"strategyId\":1001,\"awardId\":203,\"replenishCount\":2},{\"strategyId\":1002,\"awardId\":201,\"replenishCount\":20},{\"strategyId\":1002,\"awardId\":202,\"replenishCount\":10},{\"strategyId\":1002,\"awardId\":203,\"replenishCount\":2}]}"
```

补货完成后，你可以继续调用抽奖接口验证库存已生效。

## 数据初始化说明

SQL 文件里已经包含：

- `award` 奖品基础数据
- `strategy` 策略数据
- `strategy_award` 策略奖品配置
- `strategy_rule` 前置与后置规则
- `raffle_record` 示例中奖记录

Redis 不通过 SQL 初始化，而是在应用启动时根据 `strategy_award.award_surplus` 自动预热库存键。

## 压测脚本

### k6 脚本路径

- [scripts/k6/raffle-draw-load.js](scripts/k6/raffle-draw-load.js)

### Windows 运行方式

```powershell
cd D:\PythonProjects\raffle
scripts\k6\run-load-test.ps1 -BaseUrl http://127.0.0.1:8080 -StrategyId 1001 -Rps 800 -Duration 5m
```

### 通用运行方式

```bash
k6 run -e BASE_URL=http://127.0.0.1:8080 -e STRATEGY_ID=1001 scripts/k6/raffle-draw-load.js
```

### 基线测试脚本

- [scripts/k6/raffle-draw-baseline.js](scripts/k6/raffle-draw-baseline.js)
- [scripts/k6/run-baseline-test.ps1](scripts/k6/run-baseline-test.ps1)

Windows 一键执行：

```powershell
cd D:\PythonProjects\raffle
scripts\k6\run-baseline-test.ps1 -BaseUrl http://127.0.0.1:8080 -StrategyId 1001
```

默认会按 `1 RPS / 5 RPS / 20 RPS` 依次执行，持续时间默认分别为 `1m / 2m / 3m`。

如果你想单独跑某一个档位，也可以直接调用 k6：

```bash
k6 run -e BASE_URL=http://127.0.0.1:8080 -e STRATEGY_ID=1001 -e RPS=5 -e DURATION=2m scripts/k6/raffle-draw-baseline.js
```

### 阶梯测试脚本

- [scripts/k6/run-ramp-test.ps1](scripts/k6/run-ramp-test.ps1)

Windows 一键执行：

```powershell
cd D:\PythonProjects\raffle
scripts\k6\run-ramp-test.ps1 -BaseUrl http://127.0.0.1:8080 -StrategyId 1001
```

默认会依次执行：`20 / 50 / 100 / 200 / 400 / 800 RPS`，持续时间默认分别为 `1m / 1m / 1m / 1m / 1m / 3m`。

如果你想自定义档位：

```powershell
scripts\k6\run-ramp-test.ps1 -BaseUrl http://127.0.0.1:8080 -StrategyId 1001 -RpsList 10,20,40,80 -Durations 1m,1m,2m,2m
```

### 阶梯测试结果判读表

| 现象 | 典型表现 | 优先怀疑点 | 先看哪里 |
| --- | --- | --- | --- |
| 系统健康 | 实际 `http_reqs` 接近目标 RPS，`p95` 稳定且低，`dropped_iterations` 接近 0 | 暂无明显瓶颈 | 继续提升下一档 RPS |
| 明显过载 | `Insufficient VUs` + `dropped_iterations` 快速上升 + 实际 `http_reqs` 上不去 | 应用整体吞吐已到上限 | 先看应用分段耗时日志 `raffle_draw_timing` |
| 数据库瓶颈 | `persistRecord` 或库存回写阶段明显变慢，慢查询增多 | MySQL 写入、锁等待、连接池 | 慢查询日志、锁等待、连接池指标 |
| Redis/网络瓶颈 | Redis 阶段耗时抖动，网络 RTT 明显上升 | Redis 负载或网络链路 | Redis slowlog、latency、网络延迟 |
| 线程池/连接池饱和 | 响应时间上升但错误率低，吞吐平台期明显 | Web 线程池、Hikari 连接池 | 线程池活跃数、连接池等待时间 |

### 本轮结果判读示例（你这次的输出）

你这轮在 `200 / 400 / 800 RPS` 的共同特征是：

- `Insufficient VUs` 连续出现
- `dropped_iterations` 大量增加
- 实际吞吐约在 `142 ~ 161 req/s` 平台期
- `p95` 已经在 `4s` 左右
- 错误率仍为 `0%`

这个组合通常表示：系统已经过载，但还没到报错阶段，主要是排队和等待导致延迟升高。优先判断为吞吐上限已到，重点排查顺序建议：

1. 应用分段耗时日志中 `persistRecord`、`postRules` 是否显著变慢。
2. MySQL 慢查询、锁等待、连接池等待是否上升。
3. Redis 慢日志和网络延迟是否异常。

### 阶梯测试后的固定检查清单

每一档压测结束后，建议固定执行以下检查：

1. 看 k6：`http_reqs`、`p95`、`dropped_iterations`。
2. 看应用日志关键字：`raffle_draw_timing`，定位最慢阶段。
3. 看 MySQL 慢查询文件与锁等待。
4. 做库存一致性对账（Redis 与 MySQL）。

## 静态数据缓存

现在以下静态数据会优先从 Redis 读取：

- `strategy`
- `award`
- `strategy_award`
- `strategy_rule`

启动时会自动从 MySQL 预热到 Redis，之后按 `raffle.cache.refresh-interval-ms` 周期定时刷新，默认 5 分钟。

这意味着：

- `preRules`、`loadStrategy`、`selectAward` 这些阶段会减少每次请求的数据库访问。
- 库存扣减仍然会同步更新 MySQL 和 Redis，保证一致性。
- 如果你修改了配置表或奖品表，最晚在下一次定时刷新后生效。

### 可调参数

- `BASE_URL`：接口地址，默认 `http://127.0.0.1:8080`
- `STRATEGY_ID`：策略 ID，默认 `1001`
- `START_USER_ID`：用户起始 ID，默认 `200000`
- `USER_STEP`：每次递增步长，默认 `1`
- `RPS`：目标请求速率，默认 `800`
- `DURATION`：压测时长，默认 `5m`
- `PRE_ALLOCATED_VUS`：预分配虚拟用户数，默认 `300`
- `MAX_VUS`：最大虚拟用户数，默认 `2000`

## 分段耗时日志

抽奖接口现在会在日志里输出每次请求的分段耗时，关键字是 `raffle_draw_timing`。你可以在日志里直接按这个关键字筛选，快速定位慢在哪一段。

日志默认会持久化到文件：`logs/raffle-app.log`（可通过环境变量 `LOG_FILE` 覆盖）。

典型阶段包括：

- `loadStrategy`
- `preRules`
- `selectAward`
- `postRules`
- `incrementUserCount`
- `fallbackFinalize`
- `persistRecord`

示例日志：

```text
raffle_draw_timing userId=200001 strategyId=1001 awardId=201 success=true totalMs=32 stageMs=loadStrategy=4,preRules=3,selectAward=2,postRules=5,incrementUserCount=1,persistRecord=17
```

### 分段耗时统计脚本

脚本路径：

- [scripts/logs/analyze-raffle-timing.ps1](scripts/logs/analyze-raffle-timing.ps1)

示例：统计整个日志文件

```powershell
cd D:\PythonProjects\raffle
scripts\logs\analyze-raffle-timing.ps1 -LogFile logs/raffle-app.log
```

示例：只分析最近 5000 行

```powershell
scripts\logs\analyze-raffle-timing.ps1 -LogFile logs/raffle-app.log -TailLines 5000
```

输出会包含：

- 总耗时 `TOTAL avg/p95/p99`
- 每个阶段的 `avg/p95/p99`

### 每条规则耗时日志

如果你把 `org.example.raffle` 的日志级别调到 `DEBUG`，应用还会输出每条规则的耗时，关键字是 `raffle_rule_timing`。

示例：

```text
raffle_rule_timing userId=200001 strategyId=1001 actualStrategyId=1001 awardId=203 ruleModel=rule_stock costMs=7 success=true message=OK
```

这类日志适合在排查某一档压测时临时开启，用来判断到底是 `rule_blacklist`、`rule_weight`、`rule_lock` 还是 `rule_stock` 更慢。

## MySQL 慢查询日志

### 开启脚本

脚本路径：

- [src/main/resources/sql/enable-slow-query-log.sql](src/main/resources/sql/enable-slow-query-log.sql)

在 MySQL 客户端里执行这个脚本，或者逐条执行里面的 `SET GLOBAL` 语句。

### 作用

- `slow_query_log = ON`：开启慢查询日志
- `long_query_time = 0.2`：阈值设成 200ms，适合压测阶段排查
- `log_queries_not_using_indexes = ON`：辅助记录未走索引的查询

### 查看方法

先查日志文件路径：

```sql
SHOW VARIABLES LIKE 'slow_query_log_file';
```

再查看开关状态：

```sql
SHOW VARIABLES LIKE 'slow_query_log';
SHOW VARIABLES LIKE 'long_query_time';
```

如果你拿到的是 Linux 机器上的日志路径，可以直接：

```bash
tail -f /path/to/slow-query.log
```

如果是 Windows 机器，就直接用文本编辑器打开上一步查到的日志文件。

### 压测时重点看哪些 SQL

- `strategy_award` 的库存回写
- `raffle_record` 的插入
- `strategy`、`strategy_award`、`strategy_rule` 的查询
- 是否存在锁等待、死锁、连接池等待
