package org.example.raffle.repository;

import org.example.raffle.domain.RaffleRefund;

import java.util.Optional;

public interface RaffleRefundRepository {

    Optional<RaffleRefund> findByRefundId(String refundId);

    Optional<RaffleRefund> findByTaskId(Long taskId);

    RaffleRefund save(RaffleRefund raffleRefund);
}
