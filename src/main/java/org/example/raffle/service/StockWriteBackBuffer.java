package org.example.raffle.service;

public interface StockWriteBackBuffer {

    void enqueueDecrease(Long strategyId, Long awardId);
}