package com.shop.model;

public class StockAdjustmentImportItem {
    private int rowNumber;
    private String productCode;
    private String productName;
    private Double currentQty;
    private Double actualQty;
    private Double variance;
    private String reason;
    private Integer productId;
    private Long costPrice;

    private boolean productCodeError;
    private boolean actualQtyError;
    private boolean duplicateError;
    private boolean crossAdjustmentError;
    private String errorMessage;

    public int getRowNumber() { return rowNumber; }
    public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Double getCurrentQty() { return currentQty; }
    public void setCurrentQty(Double currentQty) { this.currentQty = currentQty; }

    public Double getActualQty() { return actualQty; }
    public void setActualQty(Double actualQty) { this.actualQty = actualQty; }

    public Double getVariance() { return variance; }
    public void setVariance(Double variance) { this.variance = variance; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }

    public Long getCostPrice() { return costPrice; }
    public void setCostPrice(Long costPrice) { this.costPrice = costPrice; }

    public boolean isProductCodeError() { return productCodeError; }
    public void setProductCodeError(boolean productCodeError) { this.productCodeError = productCodeError; }

    public boolean isActualQtyError() { return actualQtyError; }
    public void setActualQtyError(boolean actualQtyError) { this.actualQtyError = actualQtyError; }

    public boolean isDuplicateError() { return duplicateError; }
    public void setDuplicateError(boolean duplicateError) { this.duplicateError = duplicateError; }

    public boolean isCrossAdjustmentError() { return crossAdjustmentError; }
    public void setCrossAdjustmentError(boolean crossAdjustmentError) { this.crossAdjustmentError = crossAdjustmentError; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public boolean hasError() {
        return productCodeError || actualQtyError || duplicateError || crossAdjustmentError || (errorMessage != null && !errorMessage.isEmpty());
    }
}
