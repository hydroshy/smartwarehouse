package com.example.smartwarehouse;

public class Package {
    private String soCode;
    private String itemCode;
    private long quantity;
    private String importDate;
    private String expectedExportDate;

    public Package(String soCode, String itemCode, long quantity, String importDate, String expectedExportDate) {
        this.soCode = soCode;
        this.itemCode = itemCode;
        this.quantity = quantity;
        this.importDate = importDate;
        this.expectedExportDate = expectedExportDate;
    }

    public String getSoCode() {
        return soCode;
    }

    public String getItemCode() {
        return itemCode;
    }

    public long getQuantity() {
        return quantity;
    }

    public String getImportDate() {
        return importDate;
    }

    public String getExpectedExportDate() {
        return expectedExportDate;
    }
}