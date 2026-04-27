package org.example.raffle.service.impl;

import org.example.raffle.api.RefundApplyResponse;
import org.example.raffle.domain.RefundQuota;
import org.example.raffle.service.RefundService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("local")
public class LocalRefundService implements RefundService {

    @Override
    public RefundApplyResponse applyRefund(String refundId, Long userId, Long taskId) {
        return new RefundApplyResponse(refundId, taskId, false, "UNSUPPORTED", "refund service is not enabled in local profile", 0, 0, 0);
    }

    @Override
    public RefundQuota getQuota(Long userId, Long strategyId) {
        return new RefundQuota(userId, strategyId, 0, 0, 0);
    }
}
