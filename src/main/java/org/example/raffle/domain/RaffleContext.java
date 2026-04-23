package org.example.raffle.domain;

public class RaffleContext {

    private final Long userId;
    private final Long strategyId;
    private Long actualStrategyId;
    private Long awardId;
    private String awardName;
    private boolean success = true;
    private String message = "OK";

    public RaffleContext(Long userId, Long strategyId) {
        this.userId = userId;
        this.strategyId = strategyId;
        this.actualStrategyId = strategyId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getStrategyId() {
        return strategyId;
    }

    public Long getActualStrategyId() {
        return actualStrategyId;
    }

    public void setActualStrategyId(Long actualStrategyId) {
        this.actualStrategyId = actualStrategyId;
    }

    public Long getAwardId() {
        return awardId;
    }

    public void setAwardId(Long awardId) {
        this.awardId = awardId;
    }

    public String getAwardName() {
        return awardName;
    }

    public void setAwardName(String awardName) {
        this.awardName = awardName;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
