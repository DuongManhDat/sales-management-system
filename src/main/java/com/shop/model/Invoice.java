package com.shop.model;

public class Invoice {
    private int id;
    private String code;
    private int customerId;
    private String invoiceDate;
    private long subtotal;
    private double discountPct;
    private long discountAmt;
    private long total;
    private long paid;
    private long debt;
    private String status;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(String invoiceDate) { this.invoiceDate = invoiceDate; }

    public long getSubtotal() { return subtotal; }
    public void setSubtotal(long subtotal) { this.subtotal = subtotal; }

    public double getDiscountPct() { return discountPct; }
    public void setDiscountPct(double discountPct) { this.discountPct = discountPct; }

    public long getDiscountAmt() { return discountAmt; }
    public void setDiscountAmt(long discountAmt) { this.discountAmt = discountAmt; }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public long getPaid() { return paid; }
    public void setPaid(long paid) { this.paid = paid; }

    public long getDebt() { return debt; }
    public void setDebt(long debt) { this.debt = debt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
