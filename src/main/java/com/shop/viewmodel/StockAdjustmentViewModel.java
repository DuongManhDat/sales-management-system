package com.shop.viewmodel;

import com.shop.model.StockAdjustmentItem;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class StockAdjustmentViewModel {
    private final ObservableList<StockAdjustmentItem> items = FXCollections.observableArrayList();
    private final StringProperty note = new SimpleStringProperty("");

    public ObservableList<StockAdjustmentItem> getItems() {
        return items;
    }

    public void addItem(StockAdjustmentItem item) {
        // Prevent duplicate products in the same adjustment, or just update it
        for (StockAdjustmentItem existing : items) {
            if (existing.getProductId() == item.getProductId()) {
                existing.setActualQty(item.getActualQty());
                existing.setVariance(item.getActualQty() - existing.getCurrentQty());
                existing.setReason(item.getReason());
                // Trigger refresh if needed, but for MVP re-adding or replacing is easier
                items.remove(existing);
                items.add(existing);
                return;
            }
        }
        items.add(item);
    }
    
    public void removeItem(StockAdjustmentItem item) {
        items.remove(item);
    }

    public StringProperty noteProperty() {
        return note;
    }

    public void reset() {
        items.clear();
        note.set("");
    }
}
