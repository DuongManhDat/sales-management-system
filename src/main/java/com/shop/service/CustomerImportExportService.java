package com.shop.service;

import com.shop.dao.CustomerDao;
import com.shop.model.Customer;
import com.shop.model.CustomerImportItem;
import com.shop.model.Gender;
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
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

public class CustomerImportExportService {

    private static final Logger log = LoggerFactory.getLogger(CustomerImportExportService.class);

    private final CustomerDao customerDao = new CustomerDao();

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final DateTimeFormatter DATE_FORMATTER_VN = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_FORMATTER_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_FORMATTER_DASH = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public static class CustomerImportResult {
        private int totalRows = 0;
        private int validRows = 0;
        private int errorRows = 0;
        private final List<CustomerImportItem> allRows = new ArrayList<>();

        public int getTotalRows() { return totalRows; }
        public void setTotalRows(int totalRows) { this.totalRows = totalRows; }

        public int getValidRows() { return validRows; }
        public void setValidRows(int validRows) { this.validRows = validRows; }

        public int getErrorRows() { return errorRows; }
        public void setErrorRows(int errorRows) { this.errorRows = errorRows; }

        public List<CustomerImportItem> getAllRows() { return allRows; }

        public boolean isAllValid() {
            return errorRows == 0 && validRows > 0;
        }
    }

    public void generateTemplate(File file) throws IOException {
        log.info("Bắt đầu tạo file Excel mẫu import khách hàng tại: {}", file.getAbsolutePath());
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet mainSheet = workbook.createSheet("KhachHang");

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
                    "Mã khách hàng",
                    "Họ và tên (*)",
                    "Số điện thoại (*)",
                    "Email",
                    "Ngày sinh (dd/MM/yyyy)",
                    "Giới tính",
                    "Địa chỉ",
                    "Ghi chú"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Tạo DataFormat Text (@) để Excel giữ nguyên số 0 ở đầu khi nhập Số điện thoại và Mã KH
            DataFormat dataFormat = workbook.createDataFormat();
            CellStyle textStyle = workbook.createCellStyle();
            textStyle.setDataFormat(dataFormat.getFormat("@"));
            mainSheet.setDefaultColumnStyle(0, textStyle);
            mainSheet.setDefaultColumnStyle(2, textStyle);

            // Tạo sheet ẩn tham chiếu cho dropdown Giới tính
            Sheet refSheet = workbook.createSheet("_ThamChieu");
            Row r0 = refSheet.createRow(0);
            r0.createCell(0).setCellValue("Nam");
            Row r1 = refSheet.createRow(1);
            r1.createCell(0).setCellValue("Nữ");

            DataValidationHelper validationHelper = mainSheet.getDataValidationHelper();
            DataValidationConstraint genderConstraint = validationHelper.createFormulaListConstraint("'_ThamChieu'!$A$1:$A$2");
            CellRangeAddressList genderRange = new CellRangeAddressList(1, 1000, 5, 5);
            DataValidation genderValidation = validationHelper.createValidation(genderConstraint, genderRange);
            genderValidation.setShowErrorBox(true);
            genderValidation.setSuppressDropDownArrow(true);
            genderValidation.createErrorBox("Lỗi dữ liệu", "Vui lòng chọn giới tính Nam hoặc Nữ.");
            mainSheet.addValidationData(genderValidation);

            // Dòng ví dụ 1
            Row sampleRow1 = mainSheet.createRow(1);
            Cell c1_0 = sampleRow1.createCell(0);
            c1_0.setCellStyle(textStyle);
            c1_0.setCellValue("KH00001");
            sampleRow1.createCell(1).setCellValue("Nguyễn Văn An");
            Cell c1_2 = sampleRow1.createCell(2);
            c1_2.setCellStyle(textStyle);
            c1_2.setCellValue("0912345678");
            sampleRow1.createCell(3).setCellValue("nguyenvanan@gmail.com");
            sampleRow1.createCell(4).setCellValue("15/05/1990");
            sampleRow1.createCell(5).setCellValue("Nam");
            sampleRow1.createCell(6).setCellValue("123 Hoàng Hoa Thám, Hà Nội");
            sampleRow1.createCell(7).setCellValue("Khách hàng thân thiết");

            // Dòng ví dụ 2 (để trống mã để hệ thống tự sinh)
            Row sampleRow2 = mainSheet.createRow(2);
            Cell c2_0 = sampleRow2.createCell(0);
            c2_0.setCellStyle(textStyle);
            c2_0.setCellValue("");
            sampleRow2.createCell(1).setCellValue("Trần Thị Bình");
            Cell c2_2 = sampleRow2.createCell(2);
            c2_2.setCellStyle(textStyle);
            c2_2.setCellValue("0987654321");
            sampleRow2.createCell(3).setCellValue("binhtran@yahoo.com");
            sampleRow2.createCell(4).setCellValue("20/10/1995");
            sampleRow2.createCell(5).setCellValue("Nữ");
            sampleRow2.createCell(6).setCellValue("456 Lê Lợi, TP. Hồ Chí Minh");
            sampleRow2.createCell(7).setCellValue("Mã khách hàng sẽ tự động sinh");

            // Định dạng sẵn Text format cho các dòng tiếp theo để khi người dùng gõ SĐT vào không bị mất số 0
            for (int r = 3; r <= 500; r++) {
                Row emptyRow = mainSheet.createRow(r);
                Cell phoneCell = emptyRow.createCell(2);
                phoneCell.setCellStyle(textStyle);
            }

            // Căn chỉnh độ rộng cột
            for (int i = 0; i < headers.length; i++) {
                mainSheet.autoSizeColumn(i);
                mainSheet.setColumnWidth(i, Math.max(mainSheet.getColumnWidth(i) + 1200, 4200));
            }

            workbook.setSheetHidden(1, true);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
            log.info("Tạo file mẫu khách hàng thành công: {}", file.getAbsolutePath());
        }
    }

    public CustomerImportResult validateImport(File file) {
        log.info("Bắt đầu validate file import khách hàng: {}", file.getAbsolutePath());
        CustomerImportResult result = new CustomerImportResult();

        Set<String> existingCodes = new HashSet<>();
        Set<String> existingPhones = new HashSet<>();

        try {
            List<Customer> currentCustomers = customerDao.findAll();
            for (Customer c : currentCustomers) {
                if (c.getCode() != null && !c.getCode().trim().isEmpty()) {
                    existingCodes.add(c.getCode().trim().toUpperCase());
                }
                if (c.getPhone() != null && !c.getPhone().trim().isEmpty()) {
                    existingPhones.add(c.getPhone().trim());
                }
            }
        } catch (SQLException e) {
            log.error("Lỗi khi tải danh sách khách hàng để đối soát: {}", e.getMessage(), e);
        }

        Set<String> fileCodes = new HashSet<>();
        Set<String> filePhones = new HashSet<>();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();

            for (int i = 1; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                CustomerImportItem item = new CustomerImportItem();
                item.setRowNumber(i + 1);

                // 1. Mã khách hàng (Cột 0 - Tùy chọn)
                String code = getCellStringValue(row.getCell(0));
                item.setCode(code);
                if (!code.isEmpty()) {
                    String upperCode = code.toUpperCase();
                    if (existingCodes.contains(upperCode)) {
                        item.setCodeError(true);
                        item.addErrorMessage("Mã KH '" + code + "' đã tồn tại trong hệ thống");
                    } else if (fileCodes.contains(upperCode)) {
                        item.setCodeError(true);
                        item.addErrorMessage("Mã KH '" + code + "' bị trùng lặp trong file Excel");
                    } else {
                        fileCodes.add(upperCode);
                    }
                }

                // 2. Họ và tên (Cột 1 - Bắt buộc)
                String name = getCellStringValue(row.getCell(1));
                item.setName(name);
                if (name.isEmpty()) {
                    item.setNameError(true);
                    item.addErrorMessage("Họ tên khách hàng không được để trống");
                }

                // 3. Số điện thoại (Cột 2 - Bắt buộc: 10 chữ số)
                String phone = getCellStringValue(row.getCell(2));
                item.setPhone(phone);
                if (phone.isEmpty()) {
                    item.setPhoneError(true);
                    item.addErrorMessage("Số điện thoại không được để trống");
                } else if (!phone.matches("\\d{10}")) {
                    item.setPhoneError(true);
                    item.addErrorMessage("Số điện thoại '" + phone + "' không đúng định dạng 10 chữ số");
                } else if (existingPhones.contains(phone)) {
                    item.setPhoneError(true);
                    item.addErrorMessage("Số điện thoại '" + phone + "' đã tồn tại trong hệ thống");
                } else if (filePhones.contains(phone)) {
                    item.setPhoneError(true);
                    item.addErrorMessage("Số điện thoại '" + phone + "' bị trùng lặp trong file Excel");
                } else {
                    filePhones.add(phone);
                }

                // 4. Email (Cột 3 - Tùy chọn)
                String email = getCellStringValue(row.getCell(3));
                item.setEmail(email);
                if (!email.isEmpty() && !EMAIL_PATTERN.matcher(email).matches()) {
                    item.setEmailError(true);
                    item.addErrorMessage("Email '" + email + "' không đúng định dạng");
                }

                // 5. Ngày sinh (Cột 4 - Tùy chọn)
                Cell dobCell = row.getCell(4);
                LocalDate parsedDob = parseCellDate(dobCell);
                String dobStr = getCellStringValue(dobCell);
                item.setDateOfBirthStr(dobStr);
                if (dobCell != null && !dobStr.isEmpty()) {
                    if (parsedDob == null) {
                        item.setDobError(true);
                        item.addErrorMessage("Ngày sinh '" + dobStr + "' không đúng định dạng (dd/MM/yyyy hoặc yyyy-MM-dd)");
                    } else {
                        item.setDateOfBirth(parsedDob);
                    }
                }

                // 6. Giới tính (Cột 5 - Tùy chọn: Nam / Nữ)
                String genderStr = getCellStringValue(row.getCell(5));
                item.setGenderStr(genderStr);
                if (!genderStr.isEmpty()) {
                    if ("Nam".equalsIgnoreCase(genderStr)) {
                        item.setGender(Gender.MALE);
                    } else if ("Nữ".equalsIgnoreCase(genderStr) || "Nu".equalsIgnoreCase(genderStr)) {
                        item.setGender(Gender.FEMALE);
                    } else {
                        item.setGenderError(true);
                        item.addErrorMessage("Giới tính phải là 'Nam' hoặc 'Nữ'");
                    }
                }

                // 7. Địa chỉ (Cột 6 - Tùy chọn)
                String address = getCellStringValue(row.getCell(6));
                item.setAddress(address);

                // 8. Ghi chú (Cột 7 - Tùy chọn)
                String note = getCellStringValue(row.getCell(7));
                item.setNote(note);

                result.getAllRows().add(item);
                result.setTotalRows(result.getTotalRows() + 1);
                if (item.hasError()) {
                    result.setErrorRows(result.getErrorRows() + 1);
                } else {
                    result.setValidRows(result.getValidRows() + 1);
                }
            }

        } catch (Exception e) {
            log.error("Lỗi khi đọc file Excel khách hàng: {}", e.getMessage(), e);
            CustomerImportItem errorItem = new CustomerImportItem();
            errorItem.setRowNumber(1);
            errorItem.setNameError(true);
            errorItem.addErrorMessage("Lỗi cấu trúc file Excel: " + e.getMessage());
            result.getAllRows().add(errorItem);
            result.setErrorRows(1);
            result.setTotalRows(1);
        }

        log.info("Validate khách hàng hoàn tất: tổng {} dòng, hợp lệ {}, lỗi {}",
                result.getTotalRows(), result.getValidRows(), result.getErrorRows());
        return result;
    }

    public void executeImport(List<CustomerImportItem> items) throws SQLException {
        if (items == null || items.isEmpty()) {
            log.warn("Danh sách import khách hàng rỗng.");
            return;
        }

        for (CustomerImportItem item : items) {
            if (item.hasError()) {
                throw new IllegalStateException("Không thể import: dòng " + item.getRowNumber() + " có dữ liệu lỗi.");
            }
        }

        log.info("Bắt đầu lưu {} khách hàng vào cơ sở dữ liệu...", items.size());
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (CustomerImportItem item : items) {
                    Customer c = new Customer();
                    if (!item.getCode().isEmpty()) {
                        c.setCode(item.getCode());
                    }
                    c.setName(item.getName());
                    c.setPhone(item.getPhone());
                    c.setEmail(!item.getEmail().isEmpty() ? item.getEmail() : null);
                    c.setDateOfBirth(item.getDateOfBirth());
                    c.setGender(item.getGender());
                    c.setAddress(!item.getAddress().isEmpty() ? item.getAddress() : null);
                    c.setNote(!item.getNote().isEmpty() ? item.getNote() : null);
                    c.setActive(true);

                    customerDao.insert(conn, c);
                }
                conn.commit();
                log.info("Import thành công toàn bộ {} khách hàng.", items.size());
            } catch (SQLException e) {
                conn.rollback();
                log.error("Lỗi khi lưu danh sách khách hàng vào CSDL: {}", e.getMessage(), e);
                throw e;
            }
        }
    }

    public void exportCustomers(File file, List<Customer> customers) throws IOException {
        log.info("Bắt đầu xuất {} khách hàng ra file: {}", customers != null ? customers.size() : 0, file.getAbsolutePath());
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Danh_sach_khach_hang");

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

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(24);
            String[] headers = {
                    "Mã KH",
                    "Họ và tên",
                    "Số điện thoại",
                    "Email",
                    "Ngày sinh",
                    "Giới tính",
                    "Địa chỉ",
                    "Trạng thái",
                    "Ghi chú"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            if (customers != null) {
                for (Customer c : customers) {
                    Row row = sheet.createRow(rowIdx++);

                    Cell c0 = row.createCell(0);
                    c0.setCellValue(c.getCode() != null ? c.getCode() : "");
                    c0.setCellStyle(dataStyle);

                    Cell c1 = row.createCell(1);
                    c1.setCellValue(c.getName() != null ? c.getName() : "");
                    c1.setCellStyle(dataStyle);

                    Cell c2 = row.createCell(2);
                    c2.setCellValue(c.getPhone() != null ? c.getPhone() : "");
                    c2.setCellStyle(dataStyle);

                    Cell c3 = row.createCell(3);
                    c3.setCellValue(c.getEmail() != null ? c.getEmail() : "");
                    c3.setCellStyle(dataStyle);

                    Cell c4 = row.createCell(4);
                    c4.setCellValue(c.getDateOfBirth() != null ? c.getDateOfBirth().format(DATE_FORMATTER_VN) : "");
                    c4.setCellStyle(dataStyle);

                    Cell c5 = row.createCell(5);
                    c5.setCellValue(c.getGender() != null ? c.getGender().toString() : "");
                    c5.setCellStyle(dataStyle);

                    Cell c6 = row.createCell(6);
                    c6.setCellValue(c.getAddress() != null ? c.getAddress() : "");
                    c6.setCellStyle(dataStyle);

                    Cell c7 = row.createCell(7);
                    c7.setCellValue(c.isActive() ? "Hoạt động" : "Ngừng hoạt động");
                    c7.setCellStyle(dataStyle);

                    Cell c8 = row.createCell(8);
                    c8.setCellValue(c.getNote() != null ? c.getNote() : "");
                    c8.setCellStyle(dataStyle);
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i) + 1200, 3800));
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
            log.info("Xuất Excel khách hàng thành công: {}", file.getAbsolutePath());
        }
    }

    private LocalDate parseCellDate(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            Date date = cell.getDateCellValue();
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        String str = getCellStringValue(cell);
        if (str.isEmpty()) return null;

        try {
            return LocalDate.parse(str, DATE_FORMATTER_VN);
        } catch (Exception ignored) {}

        try {
            return LocalDate.parse(str, DATE_FORMATTER_ISO);
        } catch (Exception ignored) {}

        try {
            return LocalDate.parse(str, DATE_FORMATTER_DASH);
        } catch (Exception ignored) {}

        return null;
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
                    Date d = cell.getDateCellValue();
                    LocalDate ld = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    yield ld.format(DATE_FORMATTER_VN);
                }
                double num = cell.getNumericCellValue();
                if (num == Math.floor(num)) {
                    // Xử lý trường hợp SĐT trong Excel bị format dạng số
                    String numStr = String.valueOf((long) num);
                    if (numStr.length() == 9) {
                        yield "0" + numStr; // Thêm số 0 ở đầu nếu người dùng nhập số bị mất số 0
                    }
                    yield numStr;
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
