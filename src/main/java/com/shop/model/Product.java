package com.shop.model;

public class Product {
    private int id;
    private String code;
    private String name;
    private Integer unitId;
    private Integer categoryId;

    private long salePrice;
    private int stockQty;
    private int status;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Integer getUnitId() { return unitId; }
    public void setUnitId(Integer unitId) { this.unitId = unitId; }
    
    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    

    
    public long getSalePrice() { return salePrice; }
    public void setSalePrice(long salePrice) { this.salePrice = salePrice; }
    
    public int getStockQty() { return stockQty; }
    public void setStockQty(int stockQty) { this.stockQty = stockQty; }
    
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
}
