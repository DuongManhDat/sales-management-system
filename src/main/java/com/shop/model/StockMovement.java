package com.shop.model;

public class StockMovement {
    private int id;
    private int productId;
    private String type;
    private int qtyChange;
    private int stockAfter;
    private String refType;
    private int refId;
    private String createdAt;
    private String note;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getQtyChange() { return qtyChange; }
    public void setQtyChange(int qtyChange) { this.qtyChange = qtyChange; }

    public int getStockAfter() { return stockAfter; }
    public void setStockAfter(int stockAfter) { this.stockAfter = stockAfter; }

    public String getRefType() { return refType; }
    public void setRefType(String refType) { this.refType = refType; }

    public int getRefId() { return refId; }
    public void setRefId(int refId) { this.refId = refId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
