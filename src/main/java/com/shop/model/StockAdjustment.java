package com.shop.model;

public class StockAdjustment {
    private int id;
    private String code;
    private String adjustmentDate;
    private String note;
    private String createdAt;

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
}
