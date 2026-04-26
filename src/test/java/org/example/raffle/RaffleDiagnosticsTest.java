package org.example.raffle;

import org.example.raffle.domain.RaffleResult;
import org.example.raffle.domain.StrategyAward;
import org.example.raffle.repository.StrategyAwardRepository;
import org.example.raffle.service.RaffleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

@SpringBootTest
public class RaffleDiagnosticsTest {

    @Autowired
    private RaffleService raffleService;

    @Autowired
    private StrategyAwardRepository strategyAwardRepository;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Test
    public void diagnoseRaffleFlow() throws InterruptedException {
        System.out.println("\n=== [0. 连通性基础检查] ===");
        if (jdbcTemplate == null) {
            System.err.println("❌ 错误：数据源(DataSource)未加载，请检查数据库配置！");
        } else {
            try {
                Map<String, Object> dbCheck = jdbcTemplate.queryForMap("SELECT 1");
                System.out.println("✅ 数据库连接正常");
            } catch (Exception e) {
                System.err.println("❌ 数据库连接失败: " + e.getMessage());
            }
        }

        if (redisTemplate == null) {
            System.err.println("❌ 错误：RedisTemplate 未加载，请检查 Redis 配置！");
            return;
        }

        Long userId = 123L;
        Long strategyId = 1001L;

        System.out.println("\n=== [1. 数据库配置检查] ===");
        List<StrategyAward> awards = strategyAwardRepository.findByStrategyId(strategyId);
        if (awards.isEmpty()) {
            System.err.println("❌ 错误：数据库中找不到 strategyId = 1001 的配置！");
            return;
        }

        for (StrategyAward award : awards) {
            System.out.printf("奖品ID: %d, 名称: %s, 规则配置: %s\n", 
                award.awardId(), award.awardTitle(), award.ruleModels());
            
            if (!award.ruleModels().contains("rule_stock")) {
                System.err.println("   ⚠️ 警告：该奖品未配置 'rule_stock' 规则，将不会触发库存扣减！");
            }
        }

        System.out.println("\n=== [2. Redis 实时状态检查] ===");
        for (StrategyAward award : awards) {
            String redisKey = "lottery:award_stock:1001_" + award.awardId();
            String stock = redisTemplate.opsForValue().get(redisKey);
            System.out.printf("Key: %s, 当前Redis库存: %s\n", redisKey, stock == null ? "未初始化(NULL)" : stock);
        }

        System.out.println("\n=== [3. 执行抽奖] ===");
        try {
            RaffleResult result = raffleService.draw(userId, strategyId);
            System.out.printf("抽奖结果: %s, 获得奖品: %s (ID: %d)\n", 
                result.success() ? "成功" : "失败", result.awardName(), result.awardId());

            System.out.println("\n=== [4. 抽奖后状态校验] ===");
            String keyAfter = "lottery:award_stock:1001_" + result.awardId();
            String stockAfter = redisTemplate.opsForValue().get(keyAfter);
            System.out.println("中奖品类 Redis 最新库存: " + stockAfter);

            System.out.println("\n=== [5. 等待异步回写 (6秒)] ===");
            Thread.sleep(6000); 
            StrategyAward dbAward = strategyAwardRepository.findByStrategyIdAndAwardId(strategyId, result.awardId());
            System.out.println("数据库 (MySQL) 最新库存值: " + dbAward.awardSurplus());
        } catch (Exception e) {
            System.err.println("❌ 执行抽奖失败: " + e.getMessage());
        }
        
        System.out.println("\n=== 诊断结束 ===\n");
    }
}
