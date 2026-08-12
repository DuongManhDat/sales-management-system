package com.shop.viewmodel;

import com.shop.model.Customer;
import com.shop.model.InvoiceItem;
import com.shop.model.Product;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class PosViewModel {
    private final ObjectProperty<Customer> selectedCustomer = new SimpleObjectProperty<>();
    private final ObservableList<InvoiceItem> invoiceItems = FXCollections.observableArrayList();
    
    private final LongProperty subtotal = new SimpleLongProperty(0);
    private final DoubleProperty discountPct = new SimpleDoubleProperty(0);
    private final LongProperty discountAmt = new SimpleLongProperty(0);
    private final LongProperty total = new SimpleLongProperty(0);
    private final LongProperty customerPaid = new SimpleLongProperty(0);
    private final LongProperty changeOrDebt = new SimpleLongProperty(0);

    public PosViewModel() {
        discountPct.addListener((obs, oldVal, newVal) -> calculateTotals());
        discountAmt.addListener((obs, oldVal, newVal) -> calculateTotals());
        customerPaid.addListener((obs, oldVal, newVal) -> calculateTotals());
    }

    public void calculateTotals() {
        long currentSubtotal = invoiceItems.stream().mapToLong(InvoiceItem::getAmount).sum();
        subtotal.set(currentSubtotal);

        long currentTotal = currentSubtotal;
        if (discountAmt.get() > 0) {
            currentTotal -= discountAmt.get();
        } else if (discountPct.get() > 0) {
            currentTotal -= (long) (currentTotal * discountPct.get() / 100);
        }

        total.set(currentTotal);
        changeOrDebt.set(customerPaid.get() - currentTotal);
    }

    public void addProduct(Product product) {
        for (InvoiceItem item : invoiceItems) {
            if (item.getProductId() == product.getId()) {
                item.setQty(item.getQty() + 1);
                item.setAmount(item.getQty() * item.getSalePrice());
                calculateTotals();
                return;
            }
        }
        
        InvoiceItem item = new InvoiceItem();
        item.setProductId(product.getId());
        item.setQty(1);
        item.setSalePrice(product.getSalePrice());
        item.setAmount(product.getSalePrice());
        invoiceItems.add(item);
        calculateTotals();
    }

    public void removeProduct(InvoiceItem item) {
        invoiceItems.remove(item);
        calculateTotals();
    }

    public void updateQty(InvoiceItem item, int qty) {
        item.setQty(qty);
        item.setAmount(qty * item.getSalePrice());
        calculateTotals();
    }

    public ObjectProperty<Customer> selectedCustomerProperty() { return selectedCustomer; }
    public ObservableList<InvoiceItem> getInvoiceItems() { return invoiceItems; }
    public LongProperty subtotalProperty() { return subtotal; }
    public DoubleProperty discountPctProperty() { return discountPct; }
    public LongProperty discountAmtProperty() { return discountAmt; }
    public LongProperty totalProperty() { return total; }
    public LongProperty customerPaidProperty() { return customerPaid; }
    public LongProperty changeOrDebtProperty() { return changeOrDebt; }
}
