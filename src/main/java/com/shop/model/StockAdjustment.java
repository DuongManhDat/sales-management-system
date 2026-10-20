package com.shop.model;

public class StockAdjustment {
    private int id;
    private String code;
    private String adjustmentDate;
    private String note;
    private String createdAt;
    private String status;
    private String canceledAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    
    public String getAdjustmentDate() { return adjustmentDate; }
    public void setAdjustmentDate(String adjustmentDate) { this.adjustmentDate = adjustmentDate; }
    
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCanceledAt() { return canceledAt; }
    public void setCanceledAt(String canceledAt) { this.canceledAt = canceledAt; }
}
