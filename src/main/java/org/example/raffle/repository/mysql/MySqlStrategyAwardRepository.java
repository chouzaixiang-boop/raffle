package org.example.raffle.repository.mysql;

import org.example.raffle.domain.StrategyAward;
import org.example.raffle.repository.StrategyAwardRepository;
import org.example.raffle.repository.mysql.mapper.StrategyAwardMapper;
import org.example.raffle.repository.mysql.po.StockDeltaRow;
import org.example.raffle.repository.mysql.po.StrategyAwardRow;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

@Repository
@Profile("!local")
public class MySqlStrategyAwardRepository implements StrategyAwardRepository {

    private final StrategyAwardMapper strategyAwardMapper;

    public MySqlStrategyAwardRepository(StrategyAwardMapper strategyAwardMapper) {
        this.strategyAwardMapper = strategyAwardMapper;
    }

    @Override
    public List<StrategyAward> findByStrategyId(Long strategyId) {
        return strategyAwardMapper.findByStrategyId(strategyId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<StrategyAward> findAll() {
        return strategyAwardMapper.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public StrategyAward findByStrategyIdAndAwardId(Long strategyId, Long awardId) {
        StrategyAwardRow row = strategyAwardMapper.findByStrategyIdAndAwardId(strategyId, awardId);
        if (row == null) {
            throw new IllegalArgumentException("award config not found: strategyId=" + strategyId + ", awardId=" + awardId);
        }
        return toDomain(row);
    }

    @Override
    public void updateSurplus(Long strategyId, Long awardId, int surplus) {
        strategyAwardMapper.updateSurplus(strategyId, awardId, surplus);
    }

    public void batchDecreaseSurplus(List<StockDeltaRow> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        strategyAwardMapper.batchDecreaseSurplus(items);
    }

    private StrategyAward toDomain(StrategyAwardRow row) {
        return new StrategyAward(
                row.getStrategyId(),
                row.getAwardId(),
                row.getAwardTitle(),
                splitCsv(row.getRuleModels()),
                row.getAwardAllocate(),
                row.getAwardSurplus(),
                row.getAwardRate(),
                row.getAwardIndex()
        );
    }

    private List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }
}
