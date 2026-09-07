package com.shop.service;

import com.shop.dao.CategoryDao;
import com.shop.dao.ProductDao;
import com.shop.dao.UnitDao;
import com.shop.model.Category;
import com.shop.model.Product;
import com.shop.model.ProductImportItem;
import com.shop.model.Unit;
import com.shop.util.DBConnection;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

public class ImportExportService {

    private static final Logger log = LoggerFactory.getLogger(ImportExportService.class);

    private final ProductService productService = new ProductService();
    private final ProductDao productDao = new ProductDao();
    private final UnitDao unitDao = new UnitDao();
    private final CategoryDao categoryDao = new CategoryDao();

    public static class ImportResult {
        private int totalRows = 0;
        private int validRows = 0;
        private int errorRows = 0;
        private final List<ProductImportItem> allRows = new ArrayList<>();

        public int getTotalRows() {
            return totalRows;
        }

        public void setTotalRows(int totalRows) {
            this.totalRows = totalRows;
        }

        public int getValidRows() {
            return validRows;
        }

        public void setValidRows(int validRows) {
            this.validRows = validRows;
        }

        public int getErrorRows() {
            return errorRows;
        }

        public void setErrorRows(int errorRows) {
            this.errorRows = errorRows;
        }

        public List<ProductImportItem> getAllRows() {
            return allRows;
        }

        public boolean isAllValid() {
            return errorRows == 0 && validRows > 0;
        }
    }

    public void generateImportTemplate(File file) throws IOException {
        log.info("Bắt đầu tạo file Excel mẫu import tại: {}", file.getAbsolutePath());
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet mainSheet = workbook.createSheet("HangHoa");

            // Header styling
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 11);

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

            Row headerRow = mainSheet.createRow(0);
            headerRow.setHeightInPoints(26);
            String[] headers = {
                    "Mã hàng",
                    "Tên hàng hóa (*)",
                    "Đơn vị tính (*)",
                    "Danh mục",
                    "Giá vốn",
                    "Giá bán (*)",
                    "Tồn kho ban đầu",
                    "Ghi chú"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Lấy danh sách Đơn vị tính và Danh mục từ database
            List<Unit> units = unitDao.findAllActive();
            List<Category> categories = categoryDao.findAllActive();

            // Tạo sheet ẩn tham chiếu cho dropdown
            Sheet refSheet = workbook.createSheet("_ThamChieu");
            int maxRows = Math.max(units.size(), categories.size());
            for (int i = 0; i < maxRows; i++) {
                Row row = refSheet.createRow(i);
                if (i < units.size()) {
                    row.createCell(0).setCellValue(units.get(i).getName());
                }
                if (i < categories.size()) {
                    row.createCell(1).setCellValue(categories.get(i).getName());
                }
            }

            DataValidationHelper validationHelper = mainSheet.getDataValidationHelper();

            // Data Validation cho cột Đơn vị tính (Cột 2 / C)
            if (!units.isEmpty()) {
                String unitFormula = "'_ThamChieu'!$A$1:$A$" + units.size();
                DataValidationConstraint unitConstraint = validationHelper.createFormulaListConstraint(unitFormula);
                CellRangeAddressList unitRange = new CellRangeAddressList(1, 1000, 2, 2);
                DataValidation unitValidation = validationHelper.createValidation(unitConstraint, unitRange);
                unitValidation.setShowErrorBox(true);
                unitValidation.setSuppressDropDownArrow(true);
                unitValidation.createErrorBox("Lỗi dữ liệu", "Vui lòng chọn đơn vị tính từ danh sách có sẵn.");
                mainSheet.addValidationData(unitValidation);
            }

            // Data Validation cho cột Danh mục (Cột 3 / D)
            if (!categories.isEmpty()) {
                String catFormula = "'_ThamChieu'!$B$1:$B$" + categories.size();
                DataValidationConstraint catConstraint = validationHelper.createFormulaListConstraint(catFormula);
                CellRangeAddressList catRange = new CellRangeAddressList(1, 1000, 3, 3);
                DataValidation catValidation = validationHelper.createValidation(catConstraint, catRange);
                catValidation.setShowErrorBox(true);
                catValidation.setSuppressDropDownArrow(true);
                catValidation.createErrorBox("Lỗi dữ liệu", "Vui lòng chọn danh mục từ danh sách có sẵn.");
                mainSheet.addValidationData(catValidation);
            }

            // Dòng ví dụ 1
            String sampleUnit1 = !units.isEmpty() ? units.get(0).getName() : "Lon";
            String sampleCat1 = !categories.isEmpty() ? categories.get(0).getName() : "Nước giải khát";
            Row sampleRow1 = mainSheet.createRow(1);
            sampleRow1.createCell(0).setCellValue("SP001");
            sampleRow1.createCell(1).setCellValue("Nước ngọt Coca Cola 330ml");
            sampleRow1.createCell(2).setCellValue(sampleUnit1);
            sampleRow1.createCell(3).setCellValue(sampleCat1);
            sampleRow1.createCell(4).setCellValue(7000);
            sampleRow1.createCell(5).setCellValue(10000);
            sampleRow1.createCell(6).setCellValue(50);
            sampleRow1.createCell(7).setCellValue("Mặt hàng bán chạy");

            // Dòng ví dụ 2 (để trống mã hàng để hệ thống tự sinh)
            String sampleUnit2 = units.size() > 1 ? units.get(1).getName() : sampleUnit1;
            String sampleCat2 = categories.size() > 1 ? categories.get(1).getName() : sampleCat1;
            Row sampleRow2 = mainSheet.createRow(2);
            sampleRow2.createCell(0).setCellValue("");
            sampleRow2.createCell(1).setCellValue("Bánh quy Cosy mè 150g");
            sampleRow2.createCell(2).setCellValue(sampleUnit2);
            sampleRow2.createCell(3).setCellValue(sampleCat2);
            sampleRow2.createCell(4).setCellValue(18000);
            sampleRow2.createCell(5).setCellValue(25000);
            sampleRow2.createCell(6).setCellValue(20);
            sampleRow2.createCell(7).setCellValue("Mã hàng sẽ được tự động tạo");

            // Tự động căn chỉnh độ rộng cột
            for (int i = 0; i < headers.length; i++) {
                mainSheet.autoSizeColumn(i);
                mainSheet.setColumnWidth(i, Math.max(mainSheet.getColumnWidth(i) + 1200, 4000));
            }

            // Ẩn sheet tham chiếu
            workbook.setSheetHidden(1, true);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
            log.info("Tạo file mẫu thành công: {}", file.getAbsolutePath());
        }
    }

    public ImportResult validateImport(File file) {
        log.info("Bắt đầu validate file import: {}", file.getAbsolutePath());
        ImportResult result = new ImportResult();

        Set<String> existingCodes = new HashSet<>();
        try {
            List<Product> currentProducts = productDao.findAll();
            for (Product p : currentProducts) {
                if (p.getCode() != null && !p.getCode().trim().isEmpty()) {
                    existingCodes.add(p.getCode().trim().toUpperCase());
                }
            }
        } catch (SQLException e) {
            log.error("Lỗi khi tải danh sách mã hàng hiện tại để đối soát: {}", e.getMessage(), e);
        }

        Set<String> fileCodes = new HashSet<>();
        Set<String> fileNames = new HashSet<>();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();

            for (int i = 1; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                ProductImportItem item = new ProductImportItem();
                item.setRowNumber(i + 1); // 1-indexed trong Excel

                // 1. Mã hàng (Cột 0 - Tùy chọn)
                String code = getCellStringValue(row.getCell(0));
                item.setCode(code);
                if (!code.isEmpty()) {
                    String upperCode = code.toUpperCase();
                    if (existingCodes.contains(upperCode)) {
                        item.setCodeError(true);
                        item.addErrorMessage("Mã hàng '" + code + "' đã tồn tại trong hệ thống");
                    } else if (fileCodes.contains(upperCode)) {
                        item.setCodeError(true);
                        item.addErrorMessage("Mã hàng '" + code + "' bị trùng lặp trong file Excel");
                    } else {
                        fileCodes.add(upperCode);
                    }
                }

                // 2. Tên hàng hóa (Cột 1 - Bắt buộc)
                String name = getCellStringValue(row.getCell(1));
                item.setName(name);
                if (name.isEmpty()) {
                    item.setNameError(true);
                    item.addErrorMessage("Tên hàng hóa không được để trống");
                } else {
                    String upperName = name.toUpperCase();
                    if (fileNames.contains(upperName)) {
                        item.setNameError(true);
                        item.addErrorMessage("Tên hàng hóa '" + name + "' bị trùng lặp trong file Excel");
                    } else {
                        fileNames.add(upperName);
                    }
                }

                // 3. Đơn vị tính (Cột 2 - Bắt buộc)
                String unitName = getCellStringValue(row.getCell(2));
                item.setUnitName(unitName);
                if (unitName.isEmpty()) {
                    item.setUnitError(true);
                    item.addErrorMessage("Đơn vị tính không được để trống");
                } else {
                    Unit unit = unitDao.findByName(unitName);
                    if (unit == null) {
                        item.setUnitError(true);
                        item.addErrorMessage("Đơn vị tính '" + unitName + "' không tồn tại trong hệ thống");
                    } else {
                        item.setUnitId(unit.getId());
                    }
                }

                // 4. Danh mục (Cột 3 - Tùy chọn)
                String categoryName = getCellStringValue(row.getCell(3));
                item.setCategoryName(categoryName);
                if (!categoryName.isEmpty()) {
                    Category category = categoryDao.findByName(categoryName);
                    if (category == null) {
                        item.setCategoryError(true);
                        item.addErrorMessage("Danh mục '" + categoryName + "' không tồn tại trong hệ thống");
                    } else {
                        item.setCategoryId(category.getId());
                    }
                }

                // 5. Giá vốn (Cột 4 - Tùy chọn, mặc định 0, nếu nhập phải là số >= 0)
                Cell costCell = row.getCell(4);
                String costStr = getCellStringValue(costCell);
                item.setCostPriceStr(costStr);
                if (!costStr.isEmpty()) {
                    Long parsedCost = parseNonNegativeLong(costStr);
                    if (parsedCost == null || parsedCost == -2L) {
                        item.setCostPriceError(true);
                        item.addErrorMessage("Giá vốn không đúng định dạng số");
                    } else if (parsedCost < 0) {
                        item.setCostPriceError(true);
                        item.addErrorMessage("Giá vốn phải là số nguyên ≥ 0");
                    } else {
                        item.setCostPrice(parsedCost);
                    }
                } else {
                    item.setCostPrice(0);
                }

                // 6. Giá bán (Cột 5 - Bắt buộc số >= 0)
                Cell priceCell = row.getCell(5);
                String priceStr = getCellStringValue(priceCell);
                item.setSalePriceStr(priceStr);
                if (priceStr.isEmpty()) {
                    item.setPriceError(true);
                    item.addErrorMessage("Giá bán không được để trống");
                } else {
                    Long parsedPrice = parseNonNegativeLong(priceStr);
                    if (parsedPrice == null || parsedPrice == -2L) {
                        item.setPriceError(true);
                        item.addErrorMessage("Giá bán không đúng định dạng số");
                    } else if (parsedPrice < 0) {
                        item.setPriceError(true);
                        item.addErrorMessage("Giá bán phải là số nguyên ≥ 0");
                    } else {
                        item.setSalePrice(parsedPrice);
                    }
                }

                // 7. Tồn kho ban đầu (Cột 6 - Tùy chọn, mặc định 0, nếu nhập phải là số >= 0)
                Cell stockCell = row.getCell(6);
                String stockStr = getCellStringValue(stockCell);
                item.setInitialStockStr(stockStr);
                if (!stockStr.isEmpty()) {
                    Long parsedStock = parseNonNegativeLong(stockStr);
                    if (parsedStock == null || parsedStock == -2L) {
                        item.setStockError(true);
                        item.addErrorMessage("Tồn kho ban đầu không đúng định dạng số");
                    } else if (parsedStock < 0) {
                        item.setStockError(true);
                        item.addErrorMessage("Tồn kho ban đầu phải là số nguyên ≥ 0");
                    } else {
                        item.setInitialStock(parsedStock);
                    }
                } else {
                    item.setInitialStock(0);
                }

                // 8. Ghi chú (Cột 7 - Tùy chọn)
                String note = getCellStringValue(row.getCell(7));
                item.setNote(note);

                // Tổng hợp kết quả dòng
                result.getAllRows().add(item);
                result.setTotalRows(result.getTotalRows() + 1);
                if (item.hasError()) {
                    result.setErrorRows(result.getErrorRows() + 1);
                } else {
                    result.setValidRows(result.getValidRows() + 1);
                }
            }

        } catch (Exception e) {
            log.error("Lỗi khi đọc và validate file Excel: {}", e.getMessage(), e);
            ProductImportItem errorItem = new ProductImportItem();
            errorItem.setRowNumber(1);
            errorItem.setNameError(true);
            errorItem.addErrorMessage("Lỗi cấu trúc file Excel: " + e.getMessage());
            result.getAllRows().add(errorItem);
            result.setErrorRows(1);
            result.setTotalRows(1);
        }

        log.info("Validate hoàn tất: tổng {} dòng, hợp lệ {}, lỗi {}",
                result.getTotalRows(), result.getValidRows(), result.getErrorRows());
        return result;
    }

    private Long parseNonNegativeLong(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String clean = text.trim().replace(",", "").replace(".", "");
        if (clean.startsWith("-")) {
            return -1L; // Đánh dấu số âm
        }
        try {
            return Long.parseLong(clean);
        } catch (NumberFormatException e) {
            return -2L; // Đánh dấu sai định dạng
        }
    }

    public void executeImport(List<ProductImportItem> items) throws SQLException {
        if (items == null || items.isEmpty()) {
            log.warn("Danh sách import rỗng.");
            return;
        }

        for (ProductImportItem item : items) {
            if (item.hasError()) {
                throw new IllegalStateException("Không thể import: dòng " + item.getRowNumber() + " có dữ liệu lỗi.");
            }
        }

        log.info("Bắt đầu lưu {} hàng hóa hợp lệ vào CSDL (Kèm khởi tạo Lô kho FIFO & Thẻ kho)...", items.size());
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String insertBatchSql = "INSERT INTO inventory_batches " +
                        "(product_id, purchase_item_id, cost_price, qty_initial, qty_remaining, received_date, source, note, created_at) " +
                        "VALUES (?, NULL, ?, ?, ?, date('now', 'localtime'), 'INITIAL_STOCK', 'Khởi tạo tồn kho ban đầu từ file Excel', datetime('now', 'localtime'))";

                String insertMovementSql = "INSERT INTO stock_movements " +
                        "(product_id, type, qty_change, stock_after, ref_type, ref_id, created_at, note) " +
                        "VALUES (?, 'TONKHO_BANDAU', ?, ?, 'EXCEL_IMPORT', 0, datetime('now', 'localtime'), 'Khởi tạo tồn kho ban đầu từ file Excel')";

                try (PreparedStatement batchStmt = conn.prepareStatement(insertBatchSql);
                     PreparedStatement movementStmt = conn.prepareStatement(insertMovementSql)) {

                    for (ProductImportItem item : items) {
                        Product p = new Product();
                        if (!item.getCode().isEmpty()) {
                            p.setCode(item.getCode());
                        }
                        p.setName(item.getName());
                        p.setUnitId(item.getUnitId());
                        p.setCategoryId(item.getCategoryId());
                        p.setSalePrice(item.getSalePrice());
                        p.setStockQty(item.getInitialStock());
                        p.setNote(item.getNote());

                        int productId = productDao.insert(conn, p);
                        if (!item.getCode().isEmpty()) {
                            productDao.updateCode(conn, productId, item.getCode());
                        }

                        // Nếu có tồn kho ban đầu > 0, tạo Lô kho FIFO và Thẻ kho
                        if (item.getInitialStock() > 0) {
                            // 1. Tạo bản ghi trong inventory_batches
                            batchStmt.setInt(1, productId);
                            batchStmt.setLong(2, item.getCostPrice());
                            batchStmt.setLong(3, item.getInitialStock());
                            batchStmt.setLong(4, item.getInitialStock());
                            batchStmt.executeUpdate();

                            // 2. Ghi biến động trong stock_movements
                            movementStmt.setInt(1, productId);
                            movementStmt.setDouble(2, item.getInitialStock());
                            movementStmt.setDouble(3, item.getInitialStock());
                            movementStmt.executeUpdate();
                        }
                    }
                }
                conn.commit();
                log.info("Import thành công toàn bộ {} hàng hóa kèm Lô kho và Thẻ kho.", items.size());
            } catch (SQLException e) {
                conn.rollback();
                log.error("Lỗi khi import danh sách hàng hóa vào CSDL: {}", e.getMessage(), e);
                throw e;
            }
        }
    }

    public void exportProducts(File file, List<Product> products) throws IOException {
        log.info("Bắt đầu xuất {} hàng hóa ra file: {}", products != null ? products.size() : 0, file.getAbsolutePath());
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Danh_sach_hang_hoa");

            // Header Style
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 11);

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

            // Data Style - Text
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            // Data Style - Number
            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.cloneStyleFrom(dataStyle);
            DataFormat dataFormat = workbook.createDataFormat();
            numberStyle.setDataFormat(dataFormat.getFormat("#,##0"));
            numberStyle.setAlignment(HorizontalAlignment.RIGHT);

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(24);
            String[] headers = {
                    "Mã hàng",
                    "Tên hàng hóa",
                    "Đơn vị tính",
                    "Danh mục",
                    "Giá bán",
                    "Tồn kho",
                    "Trạng thái",
                    "Ghi chú"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            if (products != null) {
                for (Product p : products) {
                    Row row = sheet.createRow(rowIdx++);

                    Cell c0 = row.createCell(0);
                    c0.setCellValue(p.getCode() != null ? p.getCode() : "");
                    c0.setCellStyle(dataStyle);

                    Cell c1 = row.createCell(1);
                    c1.setCellValue(p.getName() != null ? p.getName() : "");
                    c1.setCellStyle(dataStyle);

                    Cell c2 = row.createCell(2);
                    c2.setCellValue(p.getUnitName() != null ? p.getUnitName() : "");
                    c2.setCellStyle(dataStyle);

                    Cell c3 = row.createCell(3);
                    c3.setCellValue(p.getCategoryName() != null ? p.getCategoryName() : "");
                    c3.setCellStyle(dataStyle);

                    Cell c4 = row.createCell(4);
                    c4.setCellValue(p.getSalePrice());
                    c4.setCellStyle(numberStyle);

                    Cell c5 = row.createCell(5);
                    c5.setCellValue(Math.round(p.getStockQty()));
                    c5.setCellStyle(numberStyle);

                    Cell c6 = row.createCell(6);
                    boolean isDeleted = p.getDeletedAt() != null;
                    c6.setCellValue(isDeleted ? "Đã ẩn" : "Hoạt động");
                    c6.setCellStyle(dataStyle);

                    Cell c7 = row.createCell(7);
                    c7.setCellValue(p.getNote() != null ? p.getNote() : "");
                    c7.setCellStyle(dataStyle);
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i) + 1000, 3500));
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
            log.info("Xuất Excel thành công: {}", file.getAbsolutePath());
        }
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = getCellStringValue(cell);
                if (!val.isEmpty()) return false;
            }
        }
        return true;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
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
                    try {
                        double num = cell.getNumericCellValue();
                        yield (num == Math.floor(num)) ? String.valueOf((long) num) : String.valueOf(num);
                    } catch (Exception ex) {
                        yield "";
                    }
                }
            }
            default -> "";
        };
    }
}
