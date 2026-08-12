package com.shop.model;

public class BatchAllocation {
    private int batchId;
    private int qty;
    private long costPrice;

    public BatchAllocation(int batchId, int qty, long costPrice) {
        this.batchId = batchId;
        this.qty = qty;
        this.costPrice = costPrice;
    }
    public int getBatchId() { return batchId; }
    public int getQty() { return qty; }
    public long getCostPrice() { return costPrice; }
}
