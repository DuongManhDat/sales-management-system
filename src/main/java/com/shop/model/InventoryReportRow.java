package com.shop.model;

public class InventoryReportRow {
    private int productId;
    private String productName;
    private int startQty;
    private int inQty;
    private int outQty;
    private int endQty;

    public InventoryReportRow(int productId, String productName, int startQty, int inQty, int outQty, int endQty) {
        this.productId = productId;
        this.productName = productName;
        this.startQty = startQty;
        this.inQty = inQty;
        this.outQty = outQty;
        this.endQty = endQty;
    }

    public InventoryReportRow() {}

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getStartQty() {
        return startQty;
    }

    public void setStartQty(int startQty) {
        this.startQty = startQty;
    }

    public int getInQty() {
        return inQty;
    }

    public void setInQty(int inQty) {
        this.inQty = inQty;
    }

    public int getOutQty() {
        return outQty;
    }

    public void setOutQty(int outQty) {
        this.outQty = outQty;
    }

    public int getEndQty() {
        return endQty;
    }

    public void setEndQty(int endQty) {
        this.endQty = endQty;
    }
}
