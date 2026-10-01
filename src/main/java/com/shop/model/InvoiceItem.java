package com.shop.model;

public class InvoiceItem {
    private int id;
    private int invoiceId;
    private int productId;
    private String productCode;
    private String productName;
    private String unitName;
    private int qty;
    private long costPrice;
    private long salePrice;
    private long discountAmt;
    private String note;
    private long amount;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getInvoiceId() { return invoiceId; }
    public void setInvoiceId(int invoiceId) { this.invoiceId = invoiceId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }

    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }

    public long getCostPrice() { return costPrice; }
    public void setCostPrice(long costPrice) { this.costPrice = costPrice; }

    public long getSalePrice() { return salePrice; }
    public void setSalePrice(long salePrice) { this.salePrice = salePrice; }

    public long getDiscountAmt() { return discountAmt; }
    public void setDiscountAmt(long discountAmt) { this.discountAmt = discountAmt; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }
}
