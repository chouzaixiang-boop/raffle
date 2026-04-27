package org.example.raffle.api;

public record RefundApplyResponse(String refundId,
                                  Long taskId,
                                  boolean accepted,
                                  String refundStatus,
                                  String message,
                                  Integer usedCount,
                                  Integer maxCount,
                                  Integer remainingCount) {
}
