package com.shop.service;

import com.shop.model.Product;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ImportExportService {
    private final ProductService productService = new ProductService();

    public static class ImportResult {
        public int totalRows = 0;
        public int validRows = 0;
        public List<String> errors = new ArrayList<>();
        public List<Product> validProducts = new ArrayList<>();
    }

    public ImportResult validateImport(File file) {
        ImportResult result = new ImportResult();
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {
             
            Sheet sheet = workbook.getSheetAt(0);
            result.totalRows = sheet.getLastRowNum(); // Row 0 is header
            
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                try {
                    Product p = new Product();
                    
                    Cell nameCell = row.getCell(0);
                    if (nameCell == null || nameCell.getStringCellValue().trim().isEmpty()) {
                        result.errors.add("Dòng " + (i+1) + ": Tên hàng hóa trống.");
                        continue;
                    }
                    p.setName(nameCell.getStringCellValue().trim());
                    
                    // Simple assume category and unit are IDs or handled manually for MVP
                    // Real MVP would lookup by name. Let's assume user inputs exact ID or we skip for now.
                    Cell unitCell = row.getCell(1);
                    if (unitCell != null && unitCell.getCellType() == CellType.NUMERIC) {
                        p.setUnitId((int) unitCell.getNumericCellValue());
                    } else {
                        result.errors.add("Dòng " + (i+1) + ": Đơn vị tính không hợp lệ (cần ID).");
                        continue;
                    }

                    Cell catCell = row.getCell(2);
                    if (catCell != null && catCell.getCellType() == CellType.NUMERIC) {
                        p.setCategoryId((int) catCell.getNumericCellValue());
                    }

                    Cell priceCell = row.getCell(3);
                    if (priceCell != null && priceCell.getCellType() == CellType.NUMERIC) {
                        p.setSalePrice((long) priceCell.getNumericCellValue());
                    } else {
                        p.setSalePrice(0);
                    }
                    
                    p.setStockQty(0); // newly imported has 0 stock
                    
                    result.validProducts.add(p);
                    result.validRows++;
                } catch (Exception ex) {
                    result.errors.add("Dòng " + (i+1) + ": " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            result.errors.add("Lỗi đọc file: " + e.getMessage());
        }
        return result;
    }

    public void executeImport(ImportResult result) throws SQLException {
        for (Product p : result.validProducts) {
            productService.addProduct(p);
        }
    }

    public void exportProducts(File file, List<Product> products) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Hàng hóa");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Mã hàng");
            header.createCell(1).setCellValue("Tên hàng hóa");
            header.createCell(2).setCellValue("Đơn vị");
            header.createCell(3).setCellValue("Danh mục");
            header.createCell(4).setCellValue("Giá bán");
            header.createCell(5).setCellValue("Tồn kho");

            int rowNum = 1;
            for (Product p : products) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(p.getCode() != null ? p.getCode() : "");
                row.createCell(1).setCellValue(p.getName());
                row.createCell(2).setCellValue(p.getUnitName() != null ? p.getUnitName() : "");
                row.createCell(3).setCellValue(p.getCategoryName() != null ? p.getCategoryName() : "");
                row.createCell(4).setCellValue(p.getSalePrice());
                row.createCell(5).setCellValue(p.getStockQty());
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
    }
}
