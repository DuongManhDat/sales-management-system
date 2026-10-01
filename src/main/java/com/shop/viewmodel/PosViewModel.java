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

    private final IntegerProperty totalQuantity = new SimpleIntegerProperty(0);
    private final IntegerProperty totalItemCount = new SimpleIntegerProperty(0);

    public PosViewModel() {
        discountPct.addListener((obs, oldVal, newVal) -> calculateTotals());
        discountAmt.addListener((obs, oldVal, newVal) -> calculateTotals());
        customerPaid.addListener((obs, oldVal, newVal) -> calculateTotals());
    }

    public void calculateTotals() {
        long currentSubtotal = 0;
        int qtySum = 0;

        for (InvoiceItem item : invoiceItems) {
            currentSubtotal += item.getAmount();
            qtySum += item.getQty();
        }

        subtotal.set(currentSubtotal);
        totalQuantity.set(qtySum);
        totalItemCount.set(invoiceItems.size());

        long currentTotal = currentSubtotal;
        if (discountAmt.get() > 0) {
            currentTotal -= discountAmt.get();
        } else if (discountPct.get() > 0) {
            currentTotal -= (long) (currentTotal * discountPct.get() / 100);
        }

        if (currentTotal < 0) {
            currentTotal = 0;
        }

        total.set(currentTotal);
        changeOrDebt.set(customerPaid.get() - currentTotal);
    }

    public void addProduct(Product product) {
        for (InvoiceItem item : invoiceItems) {
            if (item.getProductId() == product.getId()) {
                item.setQty(item.getQty() + 1);
                long lineDiscount = item.getDiscountAmt();
                long lineTotal = (item.getQty() * item.getSalePrice()) - lineDiscount;
                item.setAmount(Math.max(0, lineTotal));
                calculateTotals();
                return;
            }
        }
        
        InvoiceItem item = new InvoiceItem();
        item.setProductId(product.getId());
        item.setProductCode(product.getCode());
        item.setProductName(product.getName());
        item.setUnitName(product.getUnitName());
        item.setQty(1);
        item.setSalePrice(product.getSalePrice());
        item.setDiscountAmt(0);
        item.setAmount(product.getSalePrice());
        invoiceItems.add(item);
        calculateTotals();
    }

    public void removeProduct(InvoiceItem item) {
        invoiceItems.remove(item);
        calculateTotals();
    }

    public void updateQty(InvoiceItem item, int qty) {
        if (qty <= 0) {
            removeProduct(item);
            return;
        }
        item.setQty(qty);
        long lineDiscount = item.getDiscountAmt();
        long lineTotal = (item.getQty() * item.getSalePrice()) - lineDiscount;
        item.setAmount(Math.max(0, lineTotal));
        calculateTotals();
    }

    public void increaseQty(InvoiceItem item) {
        updateQty(item, item.getQty() + 1);
    }

    public void decreaseQty(InvoiceItem item) {
        if (item.getQty() > 1) {
            updateQty(item, item.getQty() - 1);
        } else {
            removeProduct(item);
        }
    }

    public void updateItemDiscount(InvoiceItem item, long discount) {
        item.setDiscountAmt(discount);
        long lineTotal = (item.getQty() * item.getSalePrice()) - discount;
        item.setAmount(Math.max(0, lineTotal));
        calculateTotals();
    }

    public void clearCart() {
        invoiceItems.clear();
        discountPct.set(0);
        discountAmt.set(0);
        customerPaid.set(0);
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
    public IntegerProperty totalQuantityProperty() { return totalQuantity; }
    public IntegerProperty totalItemCountProperty() { return totalItemCount; }
}
