package com.example.smartwarehouse;

public class Shelf {
    private String shelfNumber;
    private long currentQuantity;
    private String earliestExpectedExportDate; // Thêm trường để lưu ngày xuất kho sớm nhất

    public Shelf(String shelfNumber, long currentQuantity) {
        this.shelfNumber = shelfNumber;
        this.currentQuantity = currentQuantity;
        this.earliestExpectedExportDate = null;
    }

    // Constructor mới để bao gồm earliestExpectedExportDate
    public Shelf(String shelfNumber, long currentQuantity, String earliestExpectedExportDate) {
        this.shelfNumber = shelfNumber;
        this.currentQuantity = currentQuantity;
        this.earliestExpectedExportDate = earliestExpectedExportDate;
    }

    public String getShelfNumber() {
        return shelfNumber;
    }

    public long getCurrentQuantity() {
        return currentQuantity;
    }

    public String getEarliestExpectedExportDate() {
        return earliestExpectedExportDate != null ? earliestExpectedExportDate : "N/A";
    }
}