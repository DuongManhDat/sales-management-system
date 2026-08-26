package com.shop.model;

import java.time.LocalDate;

public class RevenueReportRow {
    private LocalDate date;
    private long revenue;
    private long cost;
    private long profit;

    public RevenueReportRow(LocalDate date, long revenue, long cost, long profit) {
        this.date = date;
        this.revenue = revenue;
        this.cost = cost;
        this.profit = profit;
    }

    public RevenueReportRow() {}

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public long getRevenue() {
        return revenue;
    }

    public void setRevenue(long revenue) {
        this.revenue = revenue;
    }

    public long getCost() {
        return cost;
    }

    public void setCost(long cost) {
        this.cost = cost;
    }

    public long getProfit() {
        return profit;
    }

    public void setProfit(long profit) {
        this.profit = profit;
    }
}
