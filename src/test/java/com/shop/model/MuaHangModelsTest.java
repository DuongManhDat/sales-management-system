package com.shop.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MuaHangModelsTest {
    @Test
    public void testSupplierCreation() {
        Supplier s = new Supplier(1, "NCC001", "Bia ABC", "0123", "HCM", "Note", true, "2026-07-19T10:00:00");
        assertEquals("Bia ABC", s.getName());
    }

    @Test
    public void testInventoryBatchCreation() {
        InventoryBatch b = new InventoryBatch(1, 101, null, 15000, 100, 50, "2026-07-19", "Tốt", "2026-07-19T10:00:00");
        assertEquals(50, b.getQtyRemaining());
    }
}
