package org.example.raffle.repository.mysql.po;

import java.math.BigDecimal;

public class StrategyAwardRow {

    private Long strategyId;
    private Long awardId;
    private String awardTitle;
    private String ruleModels;
    private Integer awardAllocate;
    private Integer awardSurplus;
    private BigDecimal awardRate;
    private Integer awardIndex;

    public Long getStrategyId() {
        return strategyId;
    }

    public void setStrategyId(Long strategyId) {
        this.strategyId = strategyId;
    }

    public Long getAwardId() {
        return awardId;
    }

    public void setAwardId(Long awardId) {
        this.awardId = awardId;
    }

    public String getAwardTitle() {
        return awardTitle;
    }

    public void setAwardTitle(String awardTitle) {
        this.awardTitle = awardTitle;
    }

    public String getRuleModels() {
        return ruleModels;
    }

    public void setRuleModels(String ruleModels) {
        this.ruleModels = ruleModels;
    }

    public Integer getAwardAllocate() {
        return awardAllocate;
    }

    public void setAwardAllocate(Integer awardAllocate) {
        this.awardAllocate = awardAllocate;
    }

    public Integer getAwardSurplus() {
        return awardSurplus;
    }

    public void setAwardSurplus(Integer awardSurplus) {
        this.awardSurplus = awardSurplus;
    }

    public BigDecimal getAwardRate() {
        return awardRate;
    }

    public void setAwardRate(BigDecimal awardRate) {
        this.awardRate = awardRate;
    }

    public Integer getAwardIndex() {
        return awardIndex;
    }

    public void setAwardIndex(Integer awardIndex) {
        this.awardIndex = awardIndex;
    }
}
