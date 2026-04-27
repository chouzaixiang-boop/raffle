package org.example.raffle.api;

public record RefundApplyRequest(String refundId,
                                 Long userId,
                                 Long taskId) {
}
