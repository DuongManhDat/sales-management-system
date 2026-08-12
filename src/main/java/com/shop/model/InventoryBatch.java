package com.shop.model;

public class InventoryBatch {
    private int id;
    private int productId;
    private Integer purchaseItemId;
    private long costPrice;
    private int qtyInitial;
    private int qtyRemaining;
    private String receivedDate;
    private String note;
    private String createdAt;

    public InventoryBatch() {}

    public InventoryBatch(int id, int productId, Integer purchaseItemId, long costPrice, int qtyInitial, int qtyRemaining, String receivedDate, String note, String createdAt) {
        this.id = id;
        this.productId = productId;
        this.purchaseItemId = purchaseItemId;
        this.costPrice = costPrice;
        this.qtyInitial = qtyInitial;
        this.qtyRemaining = qtyRemaining;
        this.receivedDate = receivedDate;
        this.note = note;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    public Integer getPurchaseItemId() { return purchaseItemId; }
    public void setPurchaseItemId(Integer purchaseItemId) { this.purchaseItemId = purchaseItemId; }
    public long getCostPrice() { return costPrice; }
    public void setCostPrice(long costPrice) { this.costPrice = costPrice; }
    public int getQtyInitial() { return qtyInitial; }
    public void setQtyInitial(int qtyInitial) { this.qtyInitial = qtyInitial; }
    public int getQtyRemaining() { return qtyRemaining; }
    public void setQtyRemaining(int qtyRemaining) { this.qtyRemaining = qtyRemaining; }
    public String getReceivedDate() { return receivedDate; }
    public void setReceivedDate(String receivedDate) { this.receivedDate = receivedDate; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
