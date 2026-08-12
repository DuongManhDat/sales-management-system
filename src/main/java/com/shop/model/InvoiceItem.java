package com.shop.model;

public class InvoiceItem {
    private int id;
    private int invoiceId;
    private int productId;
    private int qty;
    private long salePrice;
    private long amount;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getInvoiceId() { return invoiceId; }
    public void setInvoiceId(int invoiceId) { this.invoiceId = invoiceId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }

    public long getSalePrice() { return salePrice; }
    public void setSalePrice(long salePrice) { this.salePrice = salePrice; }

    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }
}
