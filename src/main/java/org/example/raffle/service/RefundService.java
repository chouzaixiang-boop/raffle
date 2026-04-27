package org.example.raffle.service;

import org.example.raffle.api.RefundApplyResponse;
import org.example.raffle.domain.RefundQuota;

public interface RefundService {

    RefundApplyResponse applyRefund(String refundId, Long userId, Long taskId);

    RefundQuota getQuota(Long userId, Long strategyId);
}
