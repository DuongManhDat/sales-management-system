package com.shop.service;

import com.shop.dao.ProductDao;
import com.shop.dao.StockAdjustmentDao;
import com.shop.model.Product;
import com.shop.model.StockAdjustmentImportItem;
import com.shop.model.StockAdjustmentItem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class StockAdjustmentImportExportService {
    private static final Logger log = LoggerFactory.getLogger(StockAdjustmentImportExportService.class);

    private final ProductService productService = new ProductService();
    private final StockAdjustmentDao stockAdjustmentDao = new StockAdjustmentDao();

    public static class ImportResult {
        private int totalRows;
        private int validRows;
        private int errorRows;
        private final List<StockAdjustmentImportItem> allRows = new ArrayList<>();

        public int getTotalRows() { return totalRows; }
        public void setTotalRows(int totalRows) { this.totalRows = totalRows; }

        public int getValidRows() { return validRows; }
        public void setValidRows(int validRows) { this.validRows = validRows; }

        public int getErrorRows() { return errorRows; }
        public void setErrorRows(int errorRows) { this.errorRows = errorRows; }

        public List<StockAdjustmentImportItem> getAllRows() { return allRows; }

        public boolean isAllValid() {
            return errorRows == 0 && validRows > 0;
        }
    }

    /**
     * Tạo file Excel mẫu gồm 2 sheet:
     * Sheet 1: Danh_Muc_Hang_Hoa (Toàn bộ sản phẩm đang có trong hệ thống)
     * Sheet 2: Kiem_Kho (Mẫu nhập để kiểm kho)
     */
    public void generateTemplate(File file) throws IOException, SQLException {
        log.info("Bắt đầu tạo file Excel mẫu kiểm kho tại: {}", file.getAbsolutePath());
        try (Workbook workbook = new XSSFWorkbook()) {
            // Font header
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 11);

            // Style header
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Style number & text border
            CellStyle borderStyle = workbook.createCellStyle();
            borderStyle.setBorderBottom(BorderStyle.THIN);
            borderStyle.setBorderTop(BorderStyle.THIN);
            borderStyle.setBorderLeft(BorderStyle.THIN);
            borderStyle.setBorderRight(BorderStyle.THIN);

            // 1. Tạo Sheet 1: Danh sách hàng hóa
            Sheet sheetProducts = workbook.createSheet("Danh sách hàng hóa");
            Row header1 = sheetProducts.createRow(0);
            header1.setHeightInPoints(26);
            String[] headers1 = {"Mã hàng", "Tên hàng hóa", "Danh mục"};
            for (int i = 0; i < headers1.length; i++) {
                Cell c = header1.createCell(i);
                c.setCellValue(headers1[i]);
                c.setCellStyle(headerStyle);
            }

            List<Product> products = productService.getAllProducts();
            int rowIdx1 = 1;
            for (Product p : products) {
                Row r = sheetProducts.createRow(rowIdx1++);
                Cell c0 = r.createCell(0); c0.setCellValue(p.getCode() != null ? p.getCode() : ""); c0.setCellStyle(borderStyle);
                Cell c1 = r.createCell(1); c1.setCellValue(p.getName() != null ? p.getName() : ""); c1.setCellStyle(borderStyle);
                Cell c2 = r.createCell(2); c2.setCellValue(p.getCategoryName() != null ? p.getCategoryName() : ""); c2.setCellStyle(borderStyle);
            }

            for (int i = 0; i < headers1.length; i++) {
                sheetProducts.autoSizeColumn(i);
                sheetProducts.setColumnWidth(i, Math.max(sheetProducts.getColumnWidth(i) + 1024, 4500));
            }

            // 2. Tạo Sheet 2: Kiểm kho
            Sheet sheetKiemKho = workbook.createSheet("Kiểm kho");
            Row header2 = sheetKiemKho.createRow(0);
            header2.setHeightInPoints(26);
            String[] headers2 = {"Mã hàng (*)", "Tên hàng hóa", "Tồn thực tế (*)", "Lý do kiểm / điều chỉnh"};
            for (int i = 0; i < headers2.length; i++) {
                Cell c = header2.createCell(i);
                c.setCellValue(headers2[i]);
                c.setCellStyle(headerStyle);
            }

            // Dòng dữ liệu mẫu ban đầu nếu có sản phẩm
            if (!products.isEmpty()) {
                Product sample = products.get(0);
                Row sampleRow = sheetKiemKho.createRow(1);
                Cell s0 = sampleRow.createCell(0); s0.setCellValue(sample.getCode()); s0.setCellStyle(borderStyle);
                Cell s1 = sampleRow.createCell(1); s1.setCellValue(sample.getName()); s1.setCellStyle(borderStyle);
                Cell s2 = sampleRow.createCell(2); s2.setCellValue(sample.getStockQty()); s2.setCellStyle(borderStyle);
                Cell s3 = sampleRow.createCell(3); s3.setCellValue("Kiểm đếm định kỳ"); s3.setCellStyle(borderStyle);
            }

            for (int i = 0; i < headers2.length; i++) {
                sheetKiemKho.autoSizeColumn(i);
                sheetKiemKho.setColumnWidth(i, Math.max(sheetKiemKho.getColumnWidth(i) + 1200, 4500));
            }

            // Đặt Sheet Kiểm kho là sheet được chọn mở mặc định
            workbook.setActiveSheet(1);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
            log.info("Xuất file mẫu kiểm kho thành công với {} sản phẩm trong Sheet 1.", products.size());
        }
    }

    /**
     * Đọc và kiểm tra file Excel import kiểm kho.
     * Xử lý ở Sheet 2 (hoặc sheet có tên Kiem_Kho, nếu chỉ có 1 sheet thì lấy sheet 0).
     */
    public ImportResult validateImportFile(File file) throws IOException, SQLException {
        ImportResult result = new ImportResult();
        String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // Lấy danh mục sản phẩm từ DB map theo Code (in hoa để không phân biệt hoa thường)
        List<Product> allProducts = productService.getAllProducts();
        Map<String, Product> productCodeMap = new HashMap<>();
        for (Product p : allProducts) {
            if (p.getCode() != null) {
                productCodeMap.put(p.getCode().trim().toUpperCase(), p);
            }
        }

        // Lấy danh sách các sản phẩm đã kiểm trong các phiếu COMPLETED của ngày hôm nay
        List<StockAdjustmentItem> todayItems = stockAdjustmentDao.findCompletedItemsByDate(todayStr);
        Map<Integer, Double> todayCompletedQtyMap = new HashMap<>();
        for (StockAdjustmentItem item : todayItems) {
            todayCompletedQtyMap.put(item.getProductId(), item.getActualQty());
        }

        List<StockAdjustmentImportItem> rawItems = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {

            // Ưu tiên sheet có tên 'Kiểm kho' hoặc 'Kiem_Kho' hoặc sheet thứ 2 (index 1), fallback về sheet đầu tiên
            Sheet targetSheet = workbook.getSheet("Kiểm kho");
            if (targetSheet == null) {
                targetSheet = workbook.getSheet("Kiem_Kho");
            }
            if (targetSheet == null) {
                if (workbook.getNumberOfSheets() > 1) {
                    targetSheet = workbook.getSheetAt(1);
                } else {
                    targetSheet = workbook.getSheetAt(0);
                }
            }

            int lastRow = targetSheet.getLastRowNum();
            for (int r = 1; r <= lastRow; r++) {
                Row row = targetSheet.getRow(r);
                if (row == null) continue;

                String code = getCellValueAsString(row.getCell(0));
                String name = getCellValueAsString(row.getCell(1));
                String actualQtyStr = getCellValueAsString(row.getCell(2));
                String reason = getCellValueAsString(row.getCell(3));

                // Bỏ qua dòng trống hoàn toàn
                if (code.isEmpty() && name.isEmpty() && actualQtyStr.isEmpty() && reason.isEmpty()) {
                    continue;
                }

                StockAdjustmentImportItem item = new StockAdjustmentImportItem();
                item.setRowNumber(r + 1); // 1-indexed trong Excel
                item.setProductCode(code);
                item.setProductName(name);
                item.setReason(reason);

                // 1. Kiểm tra mã hàng
                if (code.isEmpty()) {
                    item.setProductCodeError(true);
                    item.setErrorMessage("Mã hàng không được để trống");
                } else {
                    Product p = productCodeMap.get(code.toUpperCase());
                    if (p == null) {
                        item.setProductCodeError(true);
                        item.setErrorMessage("Sản phẩm mã '" + code + "' không tồn tại trong hệ thống");
                    } else {
                        item.setProductId(p.getId());
                        item.setProductName(p.getName());
                        item.setCurrentQty(p.getStockQty());
                        item.setCostPrice(p.getSalePrice());
                    }
                }

                // 2. Kiểm tra số lượng thực tế
                if (actualQtyStr.isEmpty()) {
                    item.setActualQtyError(true);
                    String err = item.getErrorMessage() != null ? item.getErrorMessage() + "; " : "";
                    item.setErrorMessage(err + "Tồn thực tế không được để trống");
                } else {
                    try {
                        double qty = Double.parseDouble(actualQtyStr.replace(",", "."));
                        if (qty < 0) {
                            item.setActualQtyError(true);
                            String err = item.getErrorMessage() != null ? item.getErrorMessage() + "; " : "";
                            item.setErrorMessage(err + "Tồn thực tế không được nhỏ hơn 0");
                        } else {
                            item.setActualQty(qty);
                            if (item.getCurrentQty() != null) {
                                item.setVariance(qty - item.getCurrentQty());
                            }
                        }
                    } catch (NumberFormatException e) {
                        item.setActualQtyError(true);
                        String err = item.getErrorMessage() != null ? item.getErrorMessage() + "; " : "";
                        item.setErrorMessage(err + "Tồn thực tế phải là số hợp lệ");
                    }
                }

                rawItems.add(item);
            }
        }

        // 3. Kiểm tra trùng lặp trong file:
        // - Nếu 2 dòng cùng sản phẩm và số lượng khác nhau -> Báo lỗi trùng cho cả 2 dòng
        // - Nếu cùng sản phẩm và cùng số lượng -> Cho phép (đánh dấu lấy dòng sau, không báo lỗi)
        Map<String, List<StockAdjustmentImportItem>> productGroups = new LinkedHashMap<>();
        for (StockAdjustmentImportItem it : rawItems) {
            if (it.getProductCode() != null && !it.getProductCode().trim().isEmpty()) {
                String key = it.getProductCode().trim().toUpperCase();
                productGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(it);
            }
        }

        for (Map.Entry<String, List<StockAdjustmentImportItem>> entry : productGroups.entrySet()) {
            List<StockAdjustmentImportItem> list = entry.getValue();
            if (list.size() > 1) {
                // Kiểm tra xem tất cả các dòng có cùng số lượng hay không
                boolean allSameQty = true;
                Double firstQty = list.get(0).getActualQty();
                for (int i = 1; i < list.size(); i++) {
                    Double currentQty = list.get(i).getActualQty();
                    if (firstQty == null || currentQty == null || Math.abs(firstQty - currentQty) > 0.0001) {
                        allSameQty = false;
                        break;
                    }
                }

                if (!allSameQty) {
                    for (StockAdjustmentImportItem it : list) {
                        it.setDuplicateError(true);
                        String err = it.getErrorMessage() != null ? it.getErrorMessage() + "; " : "";
                        it.setErrorMessage(err + "Trùng sản phẩm trong file nhưng số lượng thực tế khác nhau");
                    }
                }
            }
        }

        // 4. Kiểm tra sản phẩm đã nằm trong phiếu kiểm khác của ngày hôm đó và số lượng khác nhau
        for (StockAdjustmentImportItem it : rawItems) {
            if (it.getProductId() != null && it.getActualQty() != null) {
                Double prevQty = todayCompletedQtyMap.get(it.getProductId());
                if (prevQty != null && Math.abs(prevQty - it.getActualQty()) > 0.0001) {
                    it.setCrossAdjustmentError(true);
                    String err = it.getErrorMessage() != null ? it.getErrorMessage() + "; " : "";
                    it.setErrorMessage(err + "Sản phẩm đã nằm trong phiếu kiểm khác hôm nay với số lượng khác (" + String.format("%.2f", prevQty) + ")");
                }
            }
        }

        // Nếu trùng cùng số lượng: giữ dòng xuất hiện sau cùng
        // Lọc dedup các dòng hợp lệ
        Map<String, StockAdjustmentImportItem> dedupMap = new LinkedHashMap<>();
        List<StockAdjustmentImportItem> finalDisplayList = new ArrayList<>();

        for (StockAdjustmentImportItem it : rawItems) {
            finalDisplayList.add(it);
        }

        int validCount = 0;
        int errorCount = 0;
        for (StockAdjustmentImportItem it : finalDisplayList) {
            if (it.hasError()) {
                errorCount++;
            } else {
                validCount++;
            }
        }

        result.setTotalRows(finalDisplayList.size());
        result.setValidRows(validCount);
        result.setErrorRows(errorCount);
        result.getAllRows().addAll(finalDisplayList);

        return result;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double num = cell.getNumericCellValue();
                if (num == Math.floor(num)) {
                    yield String.valueOf((long) num);
                } else {
                    yield String.valueOf(num);
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue().trim();
                } catch (Exception e) {
                    yield String.valueOf(cell.getNumericCellValue());
                }
            }
            default -> "";
        };
    }
}
