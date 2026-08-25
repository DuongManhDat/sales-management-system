package com.shop.viewmodel;

import com.shop.model.Product;
import javafx.beans.property.*;

public class ProductFormViewModel {
    private final IntegerProperty id = new SimpleIntegerProperty(0);
    private final StringProperty code = new SimpleStringProperty("");
    private final StringProperty name = new SimpleStringProperty("");
    private final ObjectProperty<Integer> unitId = new SimpleObjectProperty<>();
    private final ObjectProperty<Integer> categoryId = new SimpleObjectProperty<>();
    private final LongProperty salePrice = new SimpleLongProperty(0);
    private final DoubleProperty stockQty = new SimpleDoubleProperty(0);
    private final StringProperty note = new SimpleStringProperty("");

    public void loadFromProduct(Product product) {
        if (product == null) {
            reset();
            return;
        }
        id.set(product.getId());
        code.set(product.getCode() != null ? product.getCode() : "");
        name.set(product.getName() != null ? product.getName() : "");
        unitId.set(product.getUnitId());
        categoryId.set(product.getCategoryId());
        salePrice.set(product.getSalePrice());
        stockQty.set(product.getStockQty());
        note.set(product.getNote() != null ? product.getNote() : "");
    }

    public void updateProduct(Product product) {
        product.setId(id.get());
        product.setCode(code.get());
        product.setName(name.get());
        product.setUnitId(unitId.get());
        product.setCategoryId(categoryId.get());
        product.setSalePrice(salePrice.get());
        product.setStockQty(stockQty.get());
        product.setNote(note.get());
    }

    public void reset() {
        id.set(0);
        code.set("");
        name.set("");
        unitId.set(null);
        categoryId.set(null);
        salePrice.set(0);
        stockQty.set(0);
        note.set("");
    }

    // Getters for properties to bind in UI
    public IntegerProperty idProperty() { return id; }
    public StringProperty codeProperty() { return code; }
    public StringProperty nameProperty() { return name; }
    public ObjectProperty<Integer> unitIdProperty() { return unitId; }
    public ObjectProperty<Integer> categoryIdProperty() { return categoryId; }
    public LongProperty salePriceProperty() { return salePrice; }
    public DoubleProperty stockQtyProperty() { return stockQty; }
    public StringProperty noteProperty() { return note; }
}
