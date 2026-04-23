package org.example.raffle.repository.mysql.po;

public class StockDeltaRow {

    private final Long strategyId;
    private final Long awardId;
    private final long decreaseCount;

    public StockDeltaRow(Long strategyId, Long awardId, long decreaseCount) {
        this.strategyId = strategyId;
        this.awardId = awardId;
        this.decreaseCount = decreaseCount;
    }

    public Long getStrategyId() {
        return strategyId;
    }

    public Long getAwardId() {
        return awardId;
    }

    public long getDecreaseCount() {
        return decreaseCount;
    }
}