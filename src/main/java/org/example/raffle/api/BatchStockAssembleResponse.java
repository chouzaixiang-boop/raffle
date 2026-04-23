package org.example.raffle.api;

import java.util.List;

public record BatchStockAssembleResponse(Integer total,
                                         Integer successCount,
                                         Integer failedCount,
                                         List<StockAssembleBatchItemResponse> results) {
}
