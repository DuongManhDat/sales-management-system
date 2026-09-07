package com.shop.viewmodel;

import com.shop.model.StockAdjustmentItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class StockAdjustmentViewModelTest {

    private StockAdjustmentViewModel viewModel;

    @BeforeEach
    public void setUp() {
        viewModel = new StockAdjustmentViewModel();
    }

    @Test
    public void testAddItem() {
        StockAdjustmentItem item = new StockAdjustmentItem();
        item.setProductId(1);
        item.setProductCode("SP01");
        item.setProductName("Sản phẩm 1");
        item.setCurrentQty(10.0);
        item.setActualQty(12.0);
        item.setVariance(2.0);
        item.setReason("Kiểm kê thừa");

        viewModel.addItem(item);

        assertEquals(1, viewModel.getItems().size());
        assertEquals("SP01", viewModel.getItems().get(0).getProductCode());
        assertEquals(12.0, viewModel.getItems().get(0).getActualQty());
    }

    @Test
    public void testAddDuplicateItemUpdatesQuantity() {
        StockAdjustmentItem item1 = new StockAdjustmentItem();
        item1.setProductId(1);
        item1.setProductCode("SP01");
        item1.setProductName("Sản phẩm 1");
        item1.setCurrentQty(10.0);
        item1.setActualQty(12.0);
        item1.setVariance(2.0);

        viewModel.addItem(item1);

        StockAdjustmentItem item2 = new StockAdjustmentItem();
        item2.setProductId(1);
        item2.setProductCode("SP01");
        item2.setProductName("Sản phẩm 1");
        item2.setCurrentQty(10.0);
        item2.setActualQty(15.0);
        item2.setVariance(5.0);
        item2.setReason("Cập nhật lại");

        viewModel.addItem(item2);

        assertEquals(1, viewModel.getItems().size());
        assertEquals(15.0, viewModel.getItems().get(0).getActualQty());
        assertEquals(5.0, viewModel.getItems().get(0).getVariance());
        assertEquals("Cập nhật lại", viewModel.getItems().get(0).getReason());
    }

    @Test
    public void testRemoveItem() {
        StockAdjustmentItem item = new StockAdjustmentItem();
        item.setProductId(1);
        item.setProductCode("SP01");
        item.setProductName("Sản phẩm 1");

        viewModel.addItem(item);
        assertEquals(1, viewModel.getItems().size());

        viewModel.removeItem(item);
        assertEquals(0, viewModel.getItems().size());
    }

    @Test
    public void testReset() {
        StockAdjustmentItem item = new StockAdjustmentItem();
        item.setProductId(1);
        viewModel.addItem(item);
        viewModel.noteProperty().set("Ghi chú test");

        viewModel.reset();

        assertEquals(0, viewModel.getItems().size());
        assertEquals("", viewModel.noteProperty().get());
    }
}
