package com.shop.viewmodel;

import com.shop.model.InvoiceItem;
import com.shop.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PosViewModelTest {
    private PosViewModel vm;

    @BeforeEach
    public void setUp() {
        vm = new PosViewModel();
    }

    @Test
    public void testPosViewModelInit() {
        assertNotNull(vm);
        assertEquals(0, vm.subtotalProperty().get());
        assertEquals(0, vm.totalQuantityProperty().get());
        assertEquals(0, vm.totalItemCountProperty().get());
    }

    @Test
    public void testAddProductAndStats() {
        Product p1 = new Product();
        p1.setId(1);
        p1.setCode("SP001");
        p1.setName("Đá hồng Gia Lai");
        p1.setSalePrice(250000);
        p1.setUnitName("Bao");

        Product p2 = new Product();
        p2.setId(2);
        p2.setCode("SP002");
        p2.setName("Áo phản quang");
        p2.setSalePrice(45000);
        p2.setUnitName("Cái");

        vm.addProduct(p1);
        vm.addProduct(p1); // qty = 2
        vm.addProduct(p2); // qty = 1

        assertEquals(2, vm.totalItemCountProperty().get(), "Should have 2 unique items");
        assertEquals(3, vm.totalQuantityProperty().get(), "Total quantity should be 3");
        assertEquals(545000, vm.subtotalProperty().get(), "Subtotal should be 2*250,000 + 45,000 = 545,000");
        assertEquals(545000, vm.totalProperty().get(), "Total should match subtotal when no discount");

        InvoiceItem item1 = vm.getInvoiceItems().get(0);
        assertEquals("SP001", item1.getProductCode());
        assertEquals("Đá hồng Gia Lai", item1.getProductName());
        assertEquals("Bao", item1.getUnitName());

        // Test increase and decrease
        vm.increaseQty(item1);
        assertEquals(3, item1.getQty());
        assertEquals(4, vm.totalQuantityProperty().get());

        vm.decreaseQty(item1);
        assertEquals(2, item1.getQty());
        assertEquals(3, vm.totalQuantityProperty().get());

        // Test remove
        vm.removeProduct(item1);
        assertEquals(1, vm.totalItemCountProperty().get());
        assertEquals(1, vm.totalQuantityProperty().get());
        assertEquals(45000, vm.subtotalProperty().get());
    }
}
