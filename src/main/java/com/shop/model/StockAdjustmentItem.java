package com.shop.model;

public class StockAdjustmentItem {
    private int id;
    private int adjustmentId;
    private int productId;
    
    // Transformed fields for view logic
    private String productCode;
    private String productName;
    
    private double currentQty;
    private double actualQty;
    private double variance;
    
    private Long costPrice;
    private String reason;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getAdjustmentId() { return adjustmentId; }
    public void setAdjustmentId(int adjustmentId) { this.adjustmentId = adjustmentId; }
    
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    
    public double getCurrentQty() { return currentQty; }
    public void setCurrentQty(double currentQty) { this.currentQty = currentQty; }
    
    public double getActualQty() { return actualQty; }
    public void setActualQty(double actualQty) { this.actualQty = actualQty; }
    
    public double getVariance() { return variance; }
    public void setVariance(double variance) { this.variance = variance; }
    
    public Long getCostPrice() { return costPrice; }
    public void setCostPrice(Long costPrice) { this.costPrice = costPrice; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
