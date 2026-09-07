package com.shop.model;

public class ProductImportItem {
    private int rowNumber;
    private String code;
    private String name;
    private String unitName;
    private String categoryName;
    private String costPriceStr;
    private long costPrice;
    private String salePriceStr;
    private long salePrice;
    private String initialStockStr;
    private long initialStock;
    private String note;

    private Integer unitId;
    private Integer categoryId;

    private boolean isCodeError;
    private boolean isNameError;
    private boolean isUnitError;
    private boolean isCategoryError;
    private boolean isCostPriceError;
    private boolean isPriceError;
    private boolean isStockError;
    private String errorMessage = "";

    public ProductImportItem() {
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getCode() {
        return code != null ? code : "";
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name != null ? name : "";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUnitName() {
        return unitName != null ? unitName : "";
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public String getCategoryName() {
        return categoryName != null ? categoryName : "";
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCostPriceStr() {
        return costPriceStr != null ? costPriceStr : "";
    }

    public void setCostPriceStr(String costPriceStr) {
        this.costPriceStr = costPriceStr;
    }

    public long getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(long costPrice) {
        this.costPrice = costPrice;
    }

    public String getSalePriceStr() {
        return salePriceStr != null ? salePriceStr : "";
    }

    public void setSalePriceStr(String salePriceStr) {
        this.salePriceStr = salePriceStr;
    }

    public long getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(long salePrice) {
        this.salePrice = salePrice;
    }

    public String getInitialStockStr() {
        return initialStockStr != null ? initialStockStr : "";
    }

    public void setInitialStockStr(String initialStockStr) {
        this.initialStockStr = initialStockStr;
    }

    public long getInitialStock() {
        return initialStock;
    }

    public void setInitialStock(long initialStock) {
        this.initialStock = initialStock;
    }

    public String getNote() {
        return note != null ? note : "";
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Integer getUnitId() {
        return unitId;
    }

    public void setUnitId(Integer unitId) {
        this.unitId = unitId;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public boolean isCodeError() {
        return isCodeError;
    }

    public void setCodeError(boolean codeError) {
        isCodeError = codeError;
    }

    public boolean isNameError() {
        return isNameError;
    }

    public void setNameError(boolean nameError) {
        isNameError = nameError;
    }

    public boolean isUnitError() {
        return isUnitError;
    }

    public void setUnitError(boolean unitError) {
        isUnitError = unitError;
    }

    public boolean isCategoryError() {
        return isCategoryError;
    }

    public void setCategoryError(boolean categoryError) {
        isCategoryError = categoryError;
    }

    public boolean isCostPriceError() {
        return isCostPriceError;
    }

    public void setCostPriceError(boolean costPriceError) {
        isCostPriceError = costPriceError;
    }

    public boolean isPriceError() {
        return isPriceError;
    }

    public void setPriceError(boolean priceError) {
        isPriceError = priceError;
    }

    public boolean isStockError() {
        return isStockError;
    }

    public void setStockError(boolean stockError) {
        isStockError = stockError;
    }

    public String getErrorMessage() {
        return errorMessage != null ? errorMessage : "";
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void addErrorMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        if (this.errorMessage == null || this.errorMessage.trim().isEmpty()) {
            this.errorMessage = message.trim();
        } else {
            this.errorMessage = this.errorMessage + "; " + message.trim();
        }
    }

    public boolean hasError() {
        return isCodeError || isNameError || isUnitError || isCategoryError || isCostPriceError || isPriceError || isStockError;
    }
}
