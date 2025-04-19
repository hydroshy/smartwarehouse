package com.example.smartwarehouse;

public class Item {
    private String itemCode;
    private String soCode;
    private String importDate;
    private String expectedExportDate;
    private String ExportDate;
    private long quantity;

    public Item(String itemCode, String soCode, String importDate, String expectedExportDate,String ExportDate, long quantity) {
        this.itemCode = itemCode;
        this.soCode = soCode;
        this.importDate = importDate;
        this.expectedExportDate = expectedExportDate;
        this.expectedExportDate = ExportDate;
        this.quantity = quantity;
    }

    public String getItemCode() {
        return itemCode;
    }

    public String getSoCode() {
        return soCode;
    }

    public String getImportDate() {
        return importDate;
    }

    public String getExpectedExportDate() {
        return expectedExportDate;
    }
    public String ExportDate() {
        return ExportDate;
    }

    public long getQuantity() {
        return quantity;
    }

    @Override
    public String toString() {
        return itemCode; // Dùng cho ArrayAdapter để hiển thị mặc định trong ListView
    }
}