package com.shop.service;

import com.shop.model.BatchAllocation;
import com.shop.model.InventoryBatch;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class InventoryServiceTest {
    
    @Test
    public void testAllocateFIFO_NoOverride() {
        // Mock batches
        List<InventoryBatch> batches = Arrays.asList(
            new InventoryBatch(1, 100, null, 10000, 50, 10, "2026-07-01", null, null),
            new InventoryBatch(2, 100, null, 12000, 50, 20, "2026-07-05", null, null)
        );
        
        InventoryService service = new InventoryService(productId -> batches);
        
        List<BatchAllocation> allocations = service.allocateFIFO(100, 25, null);
        
        assertEquals(2, allocations.size());
        assertEquals(10, allocations.get(0).getQty());
        assertEquals(10000, allocations.get(0).getCostPrice());
        
        assertEquals(15, allocations.get(1).getQty());
        assertEquals(12000, allocations.get(1).getCostPrice());
    }
}
