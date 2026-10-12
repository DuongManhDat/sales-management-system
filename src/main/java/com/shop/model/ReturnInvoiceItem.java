package com.shop.model;

public class ReturnInvoiceItem {
    private int id;
    private int returnInvoiceId;
    private int invoiceItemId;
    private int productId;
    private String productCode;
    private String productName;
    private String unitName;
    private int returnQty;
    private long refundPrice;
    private long amount;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getReturnInvoiceId() { return returnInvoiceId; }
    public void setReturnInvoiceId(int returnInvoiceId) { this.returnInvoiceId = returnInvoiceId; }

    public int getInvoiceItemId() { return invoiceItemId; }
    public void setInvoiceItemId(int invoiceItemId) { this.invoiceItemId = invoiceItemId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }

    public int getReturnQty() { return returnQty; }
    public void setReturnQty(int returnQty) { this.returnQty = returnQty; }

    public long getRefundPrice() { return refundPrice; }
    public void setRefundPrice(long refundPrice) { this.refundPrice = refundPrice; }

    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }
}
