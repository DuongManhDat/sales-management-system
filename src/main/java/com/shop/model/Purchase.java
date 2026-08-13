package com.shop.model;

public class Purchase {
    private int id;
    private String code;
    private Integer supplierId;
    private String purchaseDate;
    private long totalCost;
    private long paid;
    private long debt;
    private String status;
    private String note;
    private String createdAt;

    public Purchase() {}

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    
    public Integer getSupplierId() { return supplierId; }
    public void setSupplierId(Integer supplierId) { this.supplierId = supplierId; }
    
    public String getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(String purchaseDate) { this.purchaseDate = purchaseDate; }
    
    public long getTotalCost() { return totalCost; }
    public void setTotalCost(long totalCost) { this.totalCost = totalCost; }
    
    public long getPaid() { return paid; }
    public void setPaid(long paid) { this.paid = paid; }
    
    public long getDebt() { return debt; }
    public void setDebt(long debt) { this.debt = debt; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
