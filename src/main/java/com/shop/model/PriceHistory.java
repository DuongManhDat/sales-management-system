package com.shop.model;

public class PriceHistory {
    private int id;
    private int productId;
    private long oldPrice;
    private long newPrice;
    private String changedAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    
    public long getOldPrice() { return oldPrice; }
    public void setOldPrice(long oldPrice) { this.oldPrice = oldPrice; }
    
    public long getNewPrice() { return newPrice; }
    public void setNewPrice(long newPrice) { this.newPrice = newPrice; }
    
    public String getChangedAt() { return changedAt; }
    public void setChangedAt(String changedAt) { this.changedAt = changedAt; }
}
