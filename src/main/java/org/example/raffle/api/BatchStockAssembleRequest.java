package org.example.raffle.api;

import java.util.List;

public record BatchStockAssembleRequest(List<StockAssembleRequest> items) {
}
