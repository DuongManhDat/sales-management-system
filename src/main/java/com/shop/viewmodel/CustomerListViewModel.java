package com.shop.viewmodel;

import com.shop.model.Customer;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import java.util.List;

public class CustomerListViewModel {
    private final ObservableList<Customer> allCustomers = FXCollections.observableArrayList();
    private final FilteredList<Customer> filteredCustomers = new FilteredList<>(allCustomers, p -> true);

    private final StringProperty searchKeyword = new SimpleStringProperty("");
    private final StringProperty filterStatus = new SimpleStringProperty("Hoạt động"); // "Hoạt động", "Ngừng hoạt động", "Tất cả"

    public CustomerListViewModel() {
        searchKeyword.addListener((obs, oldVal, newVal) -> updatePredicate());
        filterStatus.addListener((obs, oldVal, newVal) -> updatePredicate());
    }

    public void setCustomers(List<Customer> customers) {
        allCustomers.setAll(customers);
        updatePredicate();
    }

    public FilteredList<Customer> getFilteredCustomers() {
        return filteredCustomers;
    }

    public StringProperty searchKeywordProperty() {
        return searchKeyword;
    }
    
    public StringProperty filterStatusProperty() {
        return filterStatus;
    }

    private void updatePredicate() {
        filteredCustomers.setPredicate(customer -> {
            String keyword = searchKeyword.get();
            String status = filterStatus.get();

            // Check Status
            boolean matchesStatus = true;
            if ("Hoạt động".equals(status)) {
                matchesStatus = customer.isActive();
            } else if ("Ngừng hoạt động".equals(status)) {
                matchesStatus = !customer.isActive();
            }

            if (!matchesStatus) {
                return false;
            }

            // Check Keyword
            if (keyword == null || keyword.trim().isEmpty()) {
                return true;
            }

            String lowerCaseFilter = keyword.toLowerCase();

            if (customer.getName().toLowerCase().contains(lowerCaseFilter)) {
                return true;
            } else if (customer.getPhone() != null && customer.getPhone().contains(lowerCaseFilter)) {
                return true;
            } else if (customer.getCode() != null && customer.getCode().toLowerCase().contains(lowerCaseFilter)) {
                return true;
            }

            return false;
        });
    }
}
