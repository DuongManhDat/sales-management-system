package com.shop.model;

public class PurchaseItem {
    private int id;
    private int purchaseId;
    private int productId;
    private int batchId;
    private int qty;
    private long costPrice;
    private long amount;

    public PurchaseItem() {}

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getPurchaseId() { return purchaseId; }
    public void setPurchaseId(int purchaseId) { this.purchaseId = purchaseId; }
    
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    
    public int getBatchId() { return batchId; }
    public void setBatchId(int batchId) { this.batchId = batchId; }
    
    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }
    
    public long getCostPrice() { return costPrice; }
    public void setCostPrice(long costPrice) { this.costPrice = costPrice; }
    
    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }
}
