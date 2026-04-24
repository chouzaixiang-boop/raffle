# 性能监控与排查指南

## 一、Linux 上的准备工作

### 1. 确保 Prometheus 和 Grafana 已启动

```bash
# 检查 Prometheus（通常 9090 端口）
curl http://localhost:9090/-/healthy

# 检查 Grafana（通常 3000 端口）
curl http://localhost:3000/api/health
```

### 2. 更新 Prometheus 配置文件

编辑你 Linux 上的 Prometheus 配置（通常在 `/etc/prometheus/prometheus.yml` 或 `/prometheus/prometheus.yml`）

将 raffle-app 的 targets 改成你 Windows 机器的 IP：

```yaml
scrape_configs:
  - job_name: 'raffle-app'
    static_configs:
      - targets: ['<YOUR_WINDOWS_IP>:8080']   # ← 改成你 Windows 机器的 IP
    metrics_path: '/actuator/prometheus'
    scrape_interval: 5s
    scrape_timeout: 5s
```

例如：如果 Windows 机器 IP 是 `192.168.80.100`，就写：
```yaml
targets: ['192.168.80.100:8080']
```

重启 Prometheus：
```bash
sudo systemctl restart prometheus
# 或
docker restart prometheus
```

验证 Prometheus 是否抓到指标：
```bash
# 在浏览器打开
http://localhost:9090/targets

# 看 raffle-app 的状态是否是 UP
```

---

## 二、导入 Grafana 面板

### 1. 在 Grafana 里创建新面板

1. 登录 Grafana（通常 http://localhost:3000，默认 admin/admin）
2. 左侧菜单 → Dashboards → Create → Import
3. 粘贴 `grafana-dashboard-complete.json` 的内容（在项目 `/monitoring/grafana-dashboard-complete.json`）
4. 点 Import

### 2. 或者直接复制 JSON 文件到 Grafana 配置目录

```bash
# 如果用 docker-compose 或容器部署的 Grafana
docker cp /path/to/grafana-dashboard-complete.json grafana:/etc/grafana/provisioning/dashboards/

# 或直接在 provisioning 目录创建
```

---

## 三、Windows 上的应用配置（已完成）

确保 `application.properties` 有这些配置：

```properties
# Actuator 暴露 Prometheus 指标
management.endpoints.web.exposure.include=prometheus,health,info
management.endpoint.health.show-details=always
management.metrics.enable.hikaricp=true
management.metrics.enable.jvm=true
management.metrics.enable.process=true
management.metrics.distribution.percentiles-histogram.raffle.persist.record.duration=true
```

应用启动时会在 `http://<WINDOWS_IP>:8080/actuator/prometheus` 暴露指标。

---

## 四、关键指标解读

### 面板 1：Hikari 连接池状态【最重要】

显示三条线：
- **Active (正在使用)** - 当前正在执行数据库操作的连接数
- **Idle (空闲)** - 可用但未使用的连接数  
- **Pending (等待中)** - 排队等待获取连接的线程数

**怎么判断**：
- 如果 `Pending` 经常 > 0，说明应用需要连接但没有可用的 → **是连接池等待**
- 如果 `Active` 长期接近 20（你的 maximum-pool-size），且 `Pending` > 0 → **数据库操作太慢**
- 如果 `Active` 很低（< 5），`Pending` 仍然 > 0 → **代码可能有连接泄漏**

### 面板 2：persistRecord 耗时【最关键的数据库操作】

显示平均耗时和最大耗时：
- `Avg (平均耗时)` - 单条 INSERT 的平均耗时
- `Max (最大耗时)` - 单条 INSERT 的最大耗时

**怎么判断**：
- 如果平均耗时 > 300ms：数据库写入很慢 → 需要深度排查 MySQL
- 如果平均耗时 < 100ms，但 Pending > 0：问题不在 persistRecord 本身，而在于排队等待连接
- 如果两者都高：既有数据库慢问题，又有连接池竞争

### 面板 3：应用吞吐量

- TPS 显示每秒的事务数
- 和 `Pending` 一起看：如果 TPS 高且 Pending > 0，说明需要加连接池大小或优化 SQL

### 面板 4：整体抽奖流程耗时

- 显示整个抽奖请求从开始到结束的耗时
- 对标你的 k6 压测结果中的 `TOTAL avg/p95`

---

## 五、故障排查流程

### 场景 1：persistRecord 耗时高（> 300ms）但 Pending = 0

**诊断**：数据库本身写入慢，不是连接池问题

**排查步骤**：

1. 在 MySQL 服务器上查看慢查询日志：
```sql
SHOW VARIABLES LIKE 'long_query_time';
SET GLOBAL long_query_time = 0.1;  -- 设置 100ms 为慢查询阈值
SHOW VARIABLES LIKE 'log_queries_not_using_indexes';
SET GLOBAL log_queries_not_using_indexes = 1;
```

2. 查看当前运行的查询：
```sql
SHOW FULL PROCESSLIST;
-- 看是否有大量 INSERT 命令在执行
```

3. 查看 InnoDB 状态：
```sql
SHOW ENGINE INNODB STATUS\G
-- 看是否有死锁、锁等待、redo log 刷盘压力
```

### 场景 2：Pending 持续 > 0 但 persistRecord 平均耗时不高（< 100ms）

**诊断**：数据库操作本身不慢，但应用拿不到连接 → 需要增加连接池大小

**修改**：
```properties
spring.datasource.hikari.maximum-pool-size=50  # 从 20 改成 50，重新压测
```

### 场景 3：Active 接近最大值 + Pending > 0 + persistRecord 耗时高

**诊断**：既是连接池竞争，也是数据库慢 → 两个问题都要解决

**修改顺序**：
1. 先加连接池：maximum-pool-size 改成 50
2. 在 MySQL 看慢查询，优化 raffle_record INSERT

---

## 六、实时观测步骤

### 第一步：启动压测

在 Windows 上运行你的 k6 脚本：
```bash
k6 run scripts/load-test.js
```

### 第二步：同时观看 Grafana 面板

1. 打开浏览器 http://localhost:3000（Linux Grafana）
2. 打开你导入的 Dashboard
3. 时间范围改成"最近 10 分钟"，自动刷新间隔改成 5 秒
4. 同时观看四个面板

### 第三步：对标压测结果

对比 k6 输出的 `persistRecord avg` 和 Grafana 的 `persistRecord Avg 耗时`：
- 如果一致：监控配置正确
- 如果 Grafana 低于 k6：可能是样本间隔问题（调小 management.metrics 采样周期）

### 第四步：根据面板判断问题

根据上面"场景 1/2/3"的诊断流程，确定是连接池、数据库写入、还是两者都慢。

---

## 七、MySQL 深度排查命令

如果要查看 raffle_record 表的性能：

```sql
-- 1. 看表的大小和索引
SHOW TABLE STATUS WHERE name='raffle_record'\G

-- 2. 看当前正在执行的查询
SELECT * FROM INFORMATION_SCHEMA.PROCESSLIST WHERE db='raffle_db';

-- 3. 查看锁等待
SELECT * FROM performance_schema.data_lock_waits;

-- 4. 查看 InnoDB 事务和锁
SELECT * FROM INFORMATION_SCHEMA.INNODB_LOCKS;
SELECT * FROM INFORMATION_SCHEMA.INNODB_LOCK_WAITS;

-- 5. 查看最近 raffle_record 的写入性能（如果打开了慢查询日志）
SHOW BINARY LOGS;
```

---

## 八、优化建议（后续）

如果确认是 persistRecord 慢（数据库写入问题），后续可以考虑：

1. **批量写入**：改成每批 100 条记录一次 INSERT（减少 IO 次数）
2. **异步写入**：使用 Redis List + 定时任务，类似库存扣减的方案
3. **表分区**：raffle_record 按日期分区，保持单个分区的数据量较小
4. **索引优化**：确保 (user_id, strategy_id, create_time) 有复合索引

---

## 九、快速参考

| 指标 | 异常值 | 可能原因 |
|------|------|--------|
| Pending > 0 持续存在 | 常见 | 连接不够或数据库慢 |
| Active ≈ 最大值 + Pending > 0 | 严重 | 连接池饱和 + 数据库慢 |
| persistRecord Avg > 300ms | 严重 | 数据库写入很慢 |
| TPS 低但 Pending = 0 | 正常 | 业务量低 |
| persistRecord Avg 低但 Pending > 0 | 警告 | 短期峰值拉高平均值 |

