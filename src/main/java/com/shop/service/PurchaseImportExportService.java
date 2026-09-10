package com.shop.service;

import com.shop.dao.ProductDao;
import com.shop.dao.PurchaseDao;
import com.shop.dao.SupplierDao;
import com.shop.model.Product;
import com.shop.model.Purchase;
import com.shop.model.PurchaseImportItem;
import com.shop.model.PurchaseItem;
import com.shop.model.Supplier;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PurchaseImportExportService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseImportExportService.class);

    private final PurchaseDao purchaseDao = new PurchaseDao();
    private final SupplierDao supplierDao = new SupplierDao();
    private final ProductDao productDao = new ProductDao();

    private static final DateTimeFormatter DATE_FORMATTER_VN = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DATE_FORMATTER_SLASH = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_FORMATTER_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static class PurchaseImportResult {
        private int totalRows = 0;
        private int validRows = 0;
        private int errorRows = 0;
        private int totalPurchases = 0;
        private final List<PurchaseImportItem> allRows = new ArrayList<>();

        public int getTotalRows() { return totalRows; }
        public void setTotalRows(int totalRows) { this.totalRows = totalRows; }

        public int getValidRows() { return validRows; }
        public void setValidRows(int validRows) { this.validRows = validRows; }

        public int getErrorRows() { return errorRows; }
        public void setErrorRows(int errorRows) { this.errorRows = errorRows; }

        public int getTotalPurchases() { return totalPurchases; }
        public void setTotalPurchases(int totalPurchases) { this.totalPurchases = totalPurchases; }

        public List<PurchaseImportItem> getAllRows() { return allRows; }

        public boolean isAllValid() {
            return errorRows == 0 && validRows > 0;
        }
    }

    public void generateTemplate(File file) throws IOException {
        log.info("Bắt đầu tạo file Excel mẫu import phiếu nhập tại: {}", file.getAbsolutePath());
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("PhieuNhap");

            // Header Font & Style
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

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(28);
            String[] headers = {
                    "Mã phiếu (*)",
                    "Mã NCC (*)",
                    "Ngày nhập (dd-mm-yyyy)",
                    "Tiền đã trả NCC",
                    "Ghi chú phiếu",
                    "Mã hàng hóa (*)",
                    "Số lượng (*)",
                    "Đơn giá nhập (*)",
                    "Ghi chú dòng"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Định dạng Text (@) cho các cột mã
            DataFormat dataFormat = workbook.createDataFormat();
            CellStyle textStyle = workbook.createCellStyle();
            textStyle.setDataFormat(dataFormat.getFormat("@"));
            sheet.setDefaultColumnStyle(0, textStyle);
            sheet.setDefaultColumnStyle(1, textStyle);
            sheet.setDefaultColumnStyle(5, textStyle);

            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.setDataFormat(dataFormat.getFormat("#,##0"));

            // Dòng mẫu 1: Phiếu 1 - Hàng hóa 1
            Row row1 = sheet.createRow(1);
            Cell c1_0 = row1.createCell(0); c1_0.setCellStyle(textStyle); c1_0.setCellValue("PN260901");
            Cell c1_1 = row1.createCell(1); c1_1.setCellStyle(textStyle); c1_1.setCellValue("NCC001");
            row1.createCell(2).setCellValue(LocalDate.now().format(DATE_FORMATTER_VN));
            Cell c1_3 = row1.createCell(3); c1_3.setCellStyle(numberStyle); c1_3.setCellValue(500000);
            row1.createCell(4).setCellValue("Nhập hàng đợt 1");
            Cell c1_5 = row1.createCell(5); c1_5.setCellStyle(textStyle); c1_5.setCellValue("HH000001");
            Cell c1_6 = row1.createCell(6); c1_6.setCellStyle(numberStyle); c1_6.setCellValue(20);
            Cell c1_7 = row1.createCell(7); c1_7.setCellStyle(numberStyle); c1_7.setCellValue(25000);
            row1.createCell(8).setCellValue("Bia lon");

            // Dòng mẫu 2: Phiếu 1 - Hàng hóa 2 (cùng mã phiếu PN260901)
            Row row2 = sheet.createRow(2);
            Cell c2_0 = row2.createCell(0); c2_0.setCellStyle(textStyle); c2_0.setCellValue("PN260901");
            Cell c2_1 = row2.createCell(1); c2_1.setCellStyle(textStyle); c2_1.setCellValue("NCC001");
            row2.createCell(2).setCellValue(LocalDate.now().format(DATE_FORMATTER_VN));
            Cell c2_3 = row2.createCell(3); c2_3.setCellStyle(numberStyle); c2_3.setCellValue(500000);
            row2.createCell(4).setCellValue("Nhập hàng đợt 1");
            Cell c2_5 = row2.createCell(5); c2_5.setCellStyle(textStyle); c2_5.setCellValue("HH000002");
            Cell c2_6 = row2.createCell(6); c2_6.setCellStyle(numberStyle); c2_6.setCellValue(10);
            Cell c2_7 = row2.createCell(7); c2_7.setCellStyle(numberStyle); c2_7.setCellValue(15000);
            row2.createCell(8).setCellValue("Nước ngọt chai");

            // Dòng mẫu 3: Phiếu 2 - Phiếu nợ
            Row row3 = sheet.createRow(3);
            Cell c3_0 = row3.createCell(0); c3_0.setCellStyle(textStyle); c3_0.setCellValue("PN260902");
            Cell c3_1 = row3.createCell(1); c3_1.setCellStyle(textStyle); c3_1.setCellValue("NCC002");
            row3.createCell(2).setCellValue(LocalDate.now().format(DATE_FORMATTER_VN));
            Cell c3_3 = row3.createCell(3); c3_3.setCellStyle(numberStyle); c3_3.setCellValue(0);
            row3.createCell(4).setCellValue("Nhập nợ trả sau");
            Cell c3_5 = row3.createCell(5); c3_5.setCellStyle(textStyle); c3_5.setCellValue("HH000001");
            Cell c3_6 = row3.createCell(6); c3_6.setCellStyle(numberStyle); c3_6.setCellValue(15);
            Cell c3_7 = row3.createCell(7); c3_7.setCellStyle(numberStyle); c3_7.setCellValue(26000);
            row3.createCell(8).setCellValue("");

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i) + 1200, 3800));
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
        log.info("Tạo file mẫu phiếu nhập thành công: {}", file.getAbsolutePath());
    }

    public PurchaseImportResult validateImportFile(File file) throws IOException {
        log.info("Bắt đầu validate file import phiếu nhập: {}", file.getAbsolutePath());
        PurchaseImportResult result = new PurchaseImportResult();

        // Cache tra cứu NCC và Sản phẩm để tăng tốc
        Map<String, Supplier> supplierCache = new HashMap<>();
        Map<String, Product> productCache = new HashMap<>();
        Map<String, PurchaseImportItem> firstRowPerPurchase = new HashMap<>();
        Set<String> distinctPurchases = new HashSet<>();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getSheet("PhieuNhap");
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }
            if (sheet == null) {
                return result;
            }

            int lastRowNum = sheet.getLastRowNum();
            for (int r = 1; r <= lastRowNum; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                PurchaseImportItem item = new PurchaseImportItem();
                item.setRowNumber(r + 1); // 1-based index cho người dùng

                String purchaseCode = getCellString(row.getCell(0)).trim();
                String supplierCode = getCellString(row.getCell(1)).trim();
                String dateStr = getCellString(row.getCell(2)).trim();
                Long paid = getCellLong(row.getCell(3), 0L);
                String purchaseNote = getCellString(row.getCell(4)).trim();

                String productCode = getCellString(row.getCell(5)).trim();
                Integer qty = getCellInteger(row.getCell(6), 0);
                Long costPrice = getCellLong(row.getCell(7), 0L);
                String itemNote = getCellString(row.getCell(8)).trim();

                item.setPurchaseCode(purchaseCode);
                item.setSupplierCode(supplierCode);
                item.setPurchaseDate(dateStr);
                item.setPaidAmount(paid != null ? paid : 0L);
                item.setPurchaseNote(purchaseNote);

                item.setProductCode(productCode);
                item.setQty(qty != null ? qty : 0);
                item.setCostPrice(costPrice != null ? costPrice : 0L);
                item.setAmount((long) item.getQty() * item.getCostPrice());
                item.setItemNote(itemNote);

                // 1. Kiểm tra Mã phiếu nhập
                if (purchaseCode.isEmpty()) {
                    item.appendError("Mã phiếu nhập không được để trống.");
                } else {
                    if (purchaseDao.existsByCode(purchaseCode)) {
                        item.appendError("Mã phiếu nhập '" + purchaseCode + "' đã tồn tại trong hệ thống.");
                    }
                    distinctPurchases.add(purchaseCode);
                }

                // 2. Kiểm tra Nhà cung cấp
                if (supplierCode.isEmpty()) {
                    item.appendError("Mã nhà cung cấp không được để trống.");
                } else {
                    Supplier supplier = supplierCache.get(supplierCode);
                    if (supplier == null && !supplierCache.containsKey(supplierCode)) {
                        try {
                            supplier = supplierDao.findByCode(supplierCode);
                            supplierCache.put(supplierCode, supplier);
                        } catch (SQLException e) {
                            log.error("Lỗi khi tìm NCC {}: {}", supplierCode, e.getMessage());
                        }
                    }
                    if (supplier == null) {
                        item.appendError("Nhà cung cấp '" + supplierCode + "' không tồn tại trong hệ thống.");
                    } else {
                        item.setSupplierId(supplier.getId());
                        item.setSupplierName(supplier.getName());
                    }
                }

                // 3. Kiểm tra Ngày nhập
                if (dateStr.isEmpty()) {
                    item.setPurchaseDate(LocalDate.now().format(DATE_FORMATTER_VN));
                } else {
                    LocalDate parsedDate = parseDate(dateStr);
                    if (parsedDate == null) {
                        item.appendError("Ngày nhập '" + dateStr + "' không đúng định dạng dd-mm-yyyy.");
                    } else {
                        item.setPurchaseDate(parsedDate.format(DATE_FORMATTER_VN));
                    }
                }

                // 4. Kiểm tra Tiền đã trả
                if (paid != null && paid < 0) {
                    item.appendError("Tiền đã trả không được là số âm.");
                }

                // 5. Kiểm tra Hàng hóa
                if (productCode.isEmpty()) {
                    item.appendError("Mã hàng hóa không được để trống.");
                } else {
                    Product product = productCache.get(productCode);
                    if (product == null && !productCache.containsKey(productCode)) {
                        try {
                            product = productDao.findByCode(productCode);
                            productCache.put(productCode, product);
                        } catch (SQLException e) {
                            log.error("Lỗi khi tìm hàng hóa {}: {}", productCode, e.getMessage());
                        }
                    }
                    if (product == null) {
                        item.appendError("Hàng hóa '" + productCode + "' không tồn tại trong danh mục.");
                    } else {
                        item.setProductId(product.getId());
                        item.setProductName(product.getName());
                        item.setUnitName(product.getUnitName());
                    }
                }

                // 6. Kiểm tra Số lượng & Giá nhập
                if (qty == null || qty <= 0) {
                    item.appendError("Số lượng nhập phải là số nguyên lớn hơn 0.");
                }
                if (costPrice == null || costPrice < 0) {
                    item.appendError("Đơn giá nhập phải là số không âm.");
                }

                // 7. Kiểm tra tính nhất quán thông tin phiếu giữa các dòng có cùng Mã phiếu
                if (!purchaseCode.isEmpty()) {
                    if (!firstRowPerPurchase.containsKey(purchaseCode)) {
                        firstRowPerPurchase.put(purchaseCode, item);
                    } else {
                        PurchaseImportItem first = firstRowPerPurchase.get(purchaseCode);
                        if (!first.getSupplierCode().equalsIgnoreCase(supplierCode)) {
                            item.appendError("Mã NCC '" + supplierCode + "' không khớp với mã NCC '" + first.getSupplierCode() + "' ở dòng " + first.getRowNumber() + " của cùng phiếu.");
                        }
                        if (!first.getPurchaseDate().equals(item.getPurchaseDate())) {
                            item.appendError("Ngày nhập không khớp với ngày '" + first.getPurchaseDate() + "' ở dòng " + first.getRowNumber() + " của cùng phiếu.");
                        }
                        if (!Objects.equals(first.getPaidAmount(), item.getPaidAmount())) {
                            item.appendError("Tiền đã trả không khớp với dòng " + first.getRowNumber() + " của cùng phiếu.");
                        }
                    }
                }

                if (item.isValid()) {
                    result.setValidRows(result.getValidRows() + 1);
                } else {
                    result.setErrorRows(result.getErrorRows() + 1);
                }
                result.setTotalRows(result.getTotalRows() + 1);
                result.getAllRows().add(item);
            }
        }

        result.setTotalPurchases(distinctPurchases.size());
        log.info("Validate phiếu nhập hoàn tất: tổng {} dòng, hợp lệ {}, lỗi {}, tổng {} phiếu.",
                result.getTotalRows(), result.getValidRows(), result.getErrorRows(), result.getTotalPurchases());
        return result;
    }

    public void executeImport(List<PurchaseImportItem> validItems) throws SQLException {
        if (validItems == null || validItems.isEmpty()) {
            throw new IllegalArgumentException("Không có dòng hợp lệ nào để nhập.");
        }

        // Gom nhóm theo Mã phiếu nhập
        Map<String, List<PurchaseImportItem>> groupedPurchases = new LinkedHashMap<>();
        for (PurchaseImportItem item : validItems) {
            groupedPurchases.computeIfAbsent(item.getPurchaseCode(), k -> new ArrayList<>()).add(item);
        }

        for (Map.Entry<String, List<PurchaseImportItem>> entry : groupedPurchases.entrySet()) {
            String purchaseCode = entry.getKey();
            List<PurchaseImportItem> groupRows = entry.getValue();

            PurchaseImportItem firstRow = groupRows.get(0);
            Purchase purchase = new Purchase();
            purchase.setCode(purchaseCode);
            purchase.setSupplierId(firstRow.getSupplierId());

            LocalDate pDate = parseDate(firstRow.getPurchaseDate());
            if (pDate == null) pDate = LocalDate.now();
            String purchaseDateStr = pDate.format(DATE_FORMATTER_VN);
            purchase.setPurchaseDate(purchaseDateStr);

            purchase.setPaid(firstRow.getPaidAmount() != null ? firstRow.getPaidAmount() : 0L);
            purchase.setNote(firstRow.getPurchaseNote());

            List<PurchaseItem> items = new ArrayList<>();
            for (PurchaseImportItem row : groupRows) {
                PurchaseItem pi = new PurchaseItem();
                pi.setProductId(row.getProductId());
                pi.setQty(row.getQty());
                pi.setCostPrice(row.getCostPrice());
                pi.setAmount((long) row.getQty() * row.getCostPrice());
                items.add(pi);
            }

            // Gọi Transaction trong DB (đã bao gồm cập nhật tồn kho, lô kho FIFO, thẻ kho stock_movements)
            new PurchaseService().createPurchase(purchase, items);
        }
        log.info("Import thành công {} phiếu nhập kho từ Excel.", groupedPurchases.size());
    }

    public void exportPurchaseList(List<Purchase> purchases, File file) throws IOException {
        log.info("Bắt đầu xuất danh sách {} phiếu nhập ra file: {}", purchases.size(), file.getAbsolutePath());
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("DanhSachPhieuNhap");

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

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(26);
            String[] headers = {
                    "STT",
                    "Mã phiếu",
                    "Ngày nhập",
                    "Nhà cung cấp",
                    "Tổng tiền (đ)",
                    "Đã thanh toán (đ)",
                    "Còn nợ (đ)",
                    "Trạng thái",
                    "Ghi chú"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DataFormat dataFormat = workbook.createDataFormat();
            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.setDataFormat(dataFormat.getFormat("#,##0"));

            CellStyle centerStyle = workbook.createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);

            int rowIndex = 1;
            for (Purchase p : purchases) {
                Row row = sheet.createRow(rowIndex);
                row.createCell(0).setCellValue(rowIndex);
                row.getCell(0).setCellStyle(centerStyle);

                row.createCell(1).setCellValue(p.getCode() != null ? p.getCode() : "");
                row.createCell(2).setCellValue(com.shop.util.FormatterUtil.formatDateToDdMmYyyy(p.getPurchaseDate()));
                row.getCell(2).setCellStyle(centerStyle);

                row.createCell(3).setCellValue(p.getSupplierName() != null ? p.getSupplierName() : "");

                Cell cTotal = row.createCell(4);
                cTotal.setCellValue(p.getTotalCost());
                cTotal.setCellStyle(numberStyle);

                Cell cPaid = row.createCell(5);
                cPaid.setCellValue(p.getPaid());
                cPaid.setCellStyle(numberStyle);

                Cell cDebt = row.createCell(6);
                cDebt.setCellValue(p.getDebt());
                cDebt.setCellStyle(numberStyle);

                row.createCell(7).setCellValue(com.shop.util.FormatterUtil.formatPurchaseStatus(p.getStatus()));
                row.getCell(7).setCellStyle(centerStyle);

                row.createCell(8).setCellValue(p.getNote() != null ? p.getNote() : "");

                rowIndex++;
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i) + 1200, 3200));
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
        log.info("Xuất danh sách phiếu nhập ra Excel thành công: {}", file.getAbsolutePath());
    }

    public void exportPurchaseDetail(Purchase purchase, List<PurchaseItem> items, File file) throws IOException {
        log.info("Bắt đầu xuất chi tiết phiếu nhập {} ra file: {}", purchase.getCode(), file.getAbsolutePath());
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("ChiTietPhieuNhap");

            // Title
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("CHI TIẾT PHIẾU NHẬP HÀNG: " + (purchase.getCode() != null ? purchase.getCode() : ""));
            titleCell.setCellStyle(titleStyle);

            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            CellStyle boldStyle = workbook.createCellStyle();
            boldStyle.setFont(boldFont);

            DataFormat dataFormat = workbook.createDataFormat();
            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.setDataFormat(dataFormat.getFormat("#,##0"));

            CellStyle centerStyle = workbook.createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Thông tin header phiếu
            Row r1 = sheet.createRow(2);
            r1.createCell(0).setCellValue("Nhà cung cấp:");
            r1.getCell(0).setCellStyle(boldStyle);
            r1.createCell(1).setCellValue(purchase.getSupplierName() != null ? purchase.getSupplierName() : "");
            r1.createCell(3).setCellValue("Ngày nhập:");
            r1.getCell(3).setCellStyle(boldStyle);
            r1.createCell(4).setCellValue(com.shop.util.FormatterUtil.formatDateToDdMmYyyy(purchase.getPurchaseDate()));

            Row r2 = sheet.createRow(3);
            r2.createCell(0).setCellValue("Trạng thái:");
            r2.getCell(0).setCellStyle(boldStyle);
            r2.createCell(1).setCellValue(com.shop.util.FormatterUtil.formatPurchaseStatus(purchase.getStatus()));
            r2.createCell(3).setCellValue("Ghi chú:");
            r2.getCell(3).setCellStyle(boldStyle);
            r2.createCell(4).setCellValue(purchase.getNote() != null ? purchase.getNote() : "");

            Row r3 = sheet.createRow(4);
            r3.createCell(0).setCellValue("Tổng tiền hàng:");
            r3.getCell(0).setCellStyle(boldStyle);
            Cell cT = r3.createCell(1); cT.setCellValue(purchase.getTotalCost()); cT.setCellStyle(numberStyle);

            r3.createCell(3).setCellValue("Đã trả NCC:");
            r3.getCell(3).setCellStyle(boldStyle);
            Cell cP = r3.createCell(4); cP.setCellValue(purchase.getPaid()); cP.setCellStyle(numberStyle);

            r3.createCell(5).setCellValue("Còn nợ:");
            r3.getCell(5).setCellStyle(boldStyle);
            Cell cD = r3.createCell(6); cD.setCellValue(purchase.getDebt()); cD.setCellStyle(numberStyle);

            // Bảng danh sách hàng hóa
            Font tableHeaderFont = workbook.createFont();
            tableHeaderFont.setBold(true);
            tableHeaderFont.setColor(IndexedColors.WHITE.getIndex());
            CellStyle tableHeaderStyle = workbook.createCellStyle();
            tableHeaderStyle.setFont(tableHeaderFont);
            tableHeaderStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            tableHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            tableHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
            tableHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            Row tableHeader = sheet.createRow(6);
            tableHeader.setHeightInPoints(24);
            String[] headers = {
                    "STT",
                    "Mã hàng hóa",
                    "Tên hàng hóa",
                    "Đơn vị tính",
                    "Số lượng",
                    "Đơn giá nhập (đ)",
                    "Thành tiền (đ)"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = tableHeader.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(tableHeaderStyle);
            }

            int itemRowIndex = 7;
            int stt = 1;
            for (PurchaseItem item : items) {
                Row row = sheet.createRow(itemRowIndex);
                row.createCell(0).setCellValue(stt);
                row.getCell(0).setCellStyle(centerStyle);

                row.createCell(1).setCellValue(item.getProductCode() != null ? item.getProductCode() : "");
                row.createCell(2).setCellValue(item.getProductName() != null ? item.getProductName() : "");
                row.createCell(3).setCellValue(item.getUnitName() != null ? item.getUnitName() : "");
                row.getCell(3).setCellStyle(centerStyle);

                Cell cQty = row.createCell(4);
                cQty.setCellValue(item.getQty());
                cQty.setCellStyle(numberStyle);

                Cell cPrice = row.createCell(5);
                cPrice.setCellValue(item.getCostPrice());
                cPrice.setCellStyle(numberStyle);

                Cell cAmount = row.createCell(6);
                cAmount.setCellValue(item.getAmount());
                cAmount.setCellStyle(numberStyle);

                stt++;
                itemRowIndex++;
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i) + 1200, 3200));
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
        log.info("Xuất chi tiết phiếu nhập ra Excel thành công: {}", file.getAbsolutePath());
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                if (!getCellString(cell).trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    LocalDate d = cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    yield d.format(DATE_FORMATTER_VN);
                } else {
                    double val = cell.getNumericCellValue();
                    if (val == (long) val) {
                        yield String.valueOf((long) val);
                    } else {
                        yield String.valueOf(val);
                    }
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        yield String.valueOf((long) cell.getNumericCellValue());
                    } catch (Exception ex) {
                        yield "";
                    }
                }
            }
            default -> "";
        };
    }

    private Long getCellLong(Cell cell, Long defaultVal) {
        if (cell == null) return defaultVal;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (long) cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                String s = cell.getStringCellValue().trim().replaceAll("[^0-9-]", "");
                if (s.isEmpty()) return defaultVal;
                return Long.parseLong(s);
            }
        } catch (Exception e) {
            return defaultVal;
        }
        return defaultVal;
    }

    private Integer getCellInteger(Cell cell, Integer defaultVal) {
        if (cell == null) return defaultVal;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                String s = cell.getStringCellValue().trim().replaceAll("[^0-9-]", "");
                if (s.isEmpty()) return defaultVal;
                return Integer.parseInt(s);
            }
        } catch (Exception e) {
            return defaultVal;
        }
        return defaultVal;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        String clean = dateStr.trim();
        try {
            return LocalDate.parse(clean, DATE_FORMATTER_VN);
        } catch (Exception ignored) {}
        try {
            return LocalDate.parse(clean, DATE_FORMATTER_SLASH);
        } catch (Exception ignored) {}
        try {
            return LocalDate.parse(clean, DATE_FORMATTER_ISO);
        } catch (Exception ignored) {}
        return null;
    }
}
