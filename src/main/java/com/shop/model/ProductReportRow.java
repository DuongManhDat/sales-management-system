package com.shop.model;

public class ProductReportRow {
    private int productId;
    private String productName;
    private int qtySold;
    private long revenue;
    private long profit;

    public ProductReportRow(int productId, String productName, int qtySold, long revenue, long profit) {
        this.productId = productId;
        this.productName = productName;
        this.qtySold = qtySold;
        this.revenue = revenue;
        this.profit = profit;
    }

    public ProductReportRow() {}

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

    public int getQtySold() {
        return qtySold;
    }

    public void setQtySold(int qtySold) {
        this.qtySold = qtySold;
    }

    public long getRevenue() {
        return revenue;
    }

    public void setRevenue(long revenue) {
        this.revenue = revenue;
    }

    public long getProfit() {
        return profit;
    }

    public void setProfit(long profit) {
        this.profit = profit;
    }
}
