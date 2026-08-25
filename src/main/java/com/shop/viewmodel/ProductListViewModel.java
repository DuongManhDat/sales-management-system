package com.shop.viewmodel;

import com.shop.model.Product;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import java.util.List;

public class ProductListViewModel {
    private final ObservableList<Product> allProducts = FXCollections.observableArrayList();
    private final FilteredList<Product> filteredProducts = new FilteredList<>(allProducts, p -> true);

    private final StringProperty searchKeyword = new SimpleStringProperty("");
    private final StringProperty filterStatus = new SimpleStringProperty("Hoạt động"); // "Hoạt động", "Đã xóa", "Tất cả"
    private final StringProperty filterCategory = new SimpleStringProperty("Tất cả");

    public ProductListViewModel() {
        searchKeyword.addListener((obs, oldVal, newVal) -> updatePredicate());
        filterStatus.addListener((obs, oldVal, newVal) -> updatePredicate());
        filterCategory.addListener((obs, oldVal, newVal) -> updatePredicate());
    }

    public void setProducts(List<Product> products) {
        allProducts.setAll(products);
        updatePredicate();
    }

    public FilteredList<Product> getFilteredProducts() {
        return filteredProducts;
    }

    public StringProperty searchKeywordProperty() {
        return searchKeyword;
    }
    
    public StringProperty filterStatusProperty() {
        return filterStatus;
    }
    
    public StringProperty filterCategoryProperty() {
        return filterCategory;
    }

    private void updatePredicate() {
        filteredProducts.setPredicate(product -> {
            String keyword = searchKeyword.get();
            String status = filterStatus.get();
            String cat = filterCategory.get();

            // Status Filter
            boolean isDeleted = product.getDeletedAt() != null;
            if ("Hoạt động".equals(status) && isDeleted) return false;
            if ("Đã xóa".equals(status) && !isDeleted) return false;

            // Category Filter
            if (!"Tất cả".equals(cat)) {
                String pCat = product.getCategoryName();
                if (pCat == null || !pCat.equals(cat)) return false;
            }

            // Keyword Filter
            if (keyword == null || keyword.trim().isEmpty()) {
                return true;
            }

            String lower = keyword.toLowerCase();
            return (product.getCode() != null && product.getCode().toLowerCase().contains(lower)) ||
                   (product.getName() != null && product.getName().toLowerCase().contains(lower));
        });
    }
}
