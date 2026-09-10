package com.shop.model;

public class PurchaseImportItem {
    private int rowNumber;
    private String purchaseCode;
    private String supplierCode;
    private String supplierName;
    private Integer supplierId;
    private String purchaseDate;
    private Long paidAmount = 0L;
    private String purchaseNote;

    private String productCode;
    private String productName;
    private Integer productId;
    private String unitName;
    private Integer qty = 0;
    private Long costPrice = 0L;
    private Long amount = 0L;
    private String itemNote;

    private boolean valid = true;
    private String errorMessage = "";

    public PurchaseImportItem() {}

    public int getRowNumber() { return rowNumber; }
    public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }

    public String getPurchaseCode() { return purchaseCode; }
    public void setPurchaseCode(String purchaseCode) { this.purchaseCode = purchaseCode; }

    public String getSupplierCode() { return supplierCode; }
    public void setSupplierCode(String supplierCode) { this.supplierCode = supplierCode; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public Integer getSupplierId() { return supplierId; }
    public void setSupplierId(Integer supplierId) { this.supplierId = supplierId; }

    public String getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(String purchaseDate) { this.purchaseDate = purchaseDate; }

    public Long getPaidAmount() { return paidAmount; }
    public void setPaidAmount(Long paidAmount) { this.paidAmount = paidAmount; }

    public String getPurchaseNote() { return purchaseNote; }
    public void setPurchaseNote(String purchaseNote) { this.purchaseNote = purchaseNote; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }

    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }

    public Integer getQty() { return qty; }
    public void setQty(Integer qty) { this.qty = qty; }

    public Long getCostPrice() { return costPrice; }
    public void setCostPrice(Long costPrice) { this.costPrice = costPrice; }

    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }

    public String getItemNote() { return itemNote; }
    public void setItemNote(String itemNote) { this.itemNote = itemNote; }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public void appendError(String error) {
        if (error == null || error.trim().isEmpty()) return;
        this.valid = false;
        if (this.errorMessage == null || this.errorMessage.trim().isEmpty()) {
            this.errorMessage = error.trim();
        } else {
            this.errorMessage += "; " + error.trim();
        }
    }

    public boolean hasError() {
        return !valid || (errorMessage != null && !errorMessage.trim().isEmpty());
    }

    public Long getLineTotal() {
        return amount != null ? amount : 0L;
    }

    public boolean isPurchaseCodeError() {
        return errorMessage != null && errorMessage.toLowerCase().contains("mã phiếu");
    }

    public boolean isSupplierCodeError() {
        return errorMessage != null && (errorMessage.toLowerCase().contains("nhà cung cấp") || errorMessage.toLowerCase().contains("mã ncc"));
    }

    public boolean isPurchaseDateError() {
        return errorMessage != null && errorMessage.toLowerCase().contains("ngày nhập");
    }

    public boolean isPaidAmountError() {
        return errorMessage != null && errorMessage.toLowerCase().contains("tiền đã trả");
    }

    public boolean isProductCodeError() {
        return errorMessage != null && (errorMessage.toLowerCase().contains("hàng hóa") || errorMessage.toLowerCase().contains("mã hàng"));
    }

    public boolean isQuantityError() {
        return errorMessage != null && errorMessage.toLowerCase().contains("số lượng");
    }

    public boolean isUnitPriceError() {
        return errorMessage != null && errorMessage.toLowerCase().contains("đơn giá");
    }
}
