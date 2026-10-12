package com.shop.model;

public class ReturnInvoice {
    private int id;
    private String code;
    private int invoiceId;
    private String invoiceCode;
    private int customerId;
    private String customerName;
    private String returnDate;
    private long totalRefund;
    private long returnFee;
    private String status;
    private String canceledAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public int getInvoiceId() { return invoiceId; }
    public void setInvoiceId(int invoiceId) { this.invoiceId = invoiceId; }

    public String getInvoiceCode() { return invoiceCode; }
    public void setInvoiceCode(String invoiceCode) { this.invoiceCode = invoiceCode; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getReturnDate() { return returnDate; }
    public void setReturnDate(String returnDate) { this.returnDate = returnDate; }

    public long getTotalRefund() { return totalRefund; }
    public void setTotalRefund(long totalRefund) { this.totalRefund = totalRefund; }

    public long getReturnFee() { return returnFee; }
    public void setReturnFee(long returnFee) { this.returnFee = returnFee; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCanceledAt() { return canceledAt; }
    public void setCanceledAt(String canceledAt) { this.canceledAt = canceledAt; }
}
