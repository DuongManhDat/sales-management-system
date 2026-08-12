package com.shop.service;

import com.shop.model.BatchAllocation;
import com.shop.model.InventoryBatch;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class InventoryService {
    
    private final Function<Integer, List<InventoryBatch>> batchFetcher;

    public InventoryService(Function<Integer, List<InventoryBatch>> batchFetcher) {
        this.batchFetcher = batchFetcher;
    }

    public List<BatchAllocation> allocateFIFO(int productId, int qtyToSell, List<BatchAllocation> override) {
        if (override != null && !override.isEmpty()) {
            return override; // Should validate sum == qtyToSell in production
        }

        List<InventoryBatch> batches = batchFetcher.apply(productId);
        List<BatchAllocation> allocations = new ArrayList<>();
        int qtyLeft = qtyToSell;

        for (InventoryBatch batch : batches) {
            int take = Math.min(batch.getQtyRemaining(), qtyLeft);
            if (take > 0) {
                allocations.add(new BatchAllocation(batch.getId(), take, batch.getCostPrice()));
                qtyLeft -= take;
            }
            if (qtyLeft == 0) break;
        }

        if (qtyLeft > 0 && !batches.isEmpty()) {
            InventoryBatch lastBatch = batches.get(batches.size() - 1);
            allocations.add(new BatchAllocation(lastBatch.getId(), qtyLeft, lastBatch.getCostPrice()));
        }

        return allocations;
    }
}
