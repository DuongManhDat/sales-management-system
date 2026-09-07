package com.shop.service;

import com.shop.infra.db.SchemaInitializer;
import com.shop.model.Product;
import com.shop.model.ProductImportItem;
import com.shop.util.DBConnection;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ImportExportServiceTest {

    private ImportExportService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        System.setProperty("db.url", "jdbc:sqlite::memory:");
        SchemaInitializer.initialize();
        service = new ImportExportService();
    }

    @Test
    void testGenerateImportTemplate() throws IOException {
        File templateFile = tempDir.resolve("template.xlsx").toFile();
        service.generateImportTemplate(templateFile);

        assertTrue(templateFile.exists());
        assertTrue(templateFile.length() > 0);

        try (FileInputStream fis = new FileInputStream(templateFile);
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheet("HangHoa");
            assertNotNull(sheet);

            Row headerRow = sheet.getRow(0);
            assertNotNull(headerRow);
            assertEquals("Mã hàng", headerRow.getCell(0).getStringCellValue());
            assertEquals("Tên hàng hóa (*)", headerRow.getCell(1).getStringCellValue());
            assertEquals("Đơn vị tính (*)", headerRow.getCell(2).getStringCellValue());
            assertEquals("Danh mục", headerRow.getCell(3).getStringCellValue());
            assertEquals("Giá vốn", headerRow.getCell(4).getStringCellValue());
            assertEquals("Giá bán (*)", headerRow.getCell(5).getStringCellValue());
            assertEquals("Tồn kho ban đầu", headerRow.getCell(6).getStringCellValue());
            assertEquals("Ghi chú", headerRow.getCell(7).getStringCellValue());

            // Có ít nhất 2 dòng ví dụ
            assertNotNull(sheet.getRow(1));
            assertNotNull(sheet.getRow(2));
        }
    }

    @Test
    void testExportProducts() throws IOException {
        File exportFile = tempDir.resolve("export.xlsx").toFile();

        List<Product> products = new ArrayList<>();
        Product p = new Product();
        p.setCode("HH000001");
        p.setName("Sản phẩm A");
        p.setUnitName("Cái");
        p.setCategoryName("Gia dụng");
        p.setSalePrice(50000);
        p.setStockQty(12);
        p.setNote("Ghi chú thử nghiệm");
        products.add(p);

        service.exportProducts(exportFile, products);

        assertTrue(exportFile.exists());
        assertTrue(exportFile.length() > 0);

        try (FileInputStream fis = new FileInputStream(exportFile);
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheet("Danh_sach_hang_hoa");
            assertNotNull(sheet);

            Row row1 = sheet.getRow(1);
            assertNotNull(row1);
            assertEquals("HH000001", row1.getCell(0).getStringCellValue());
            assertEquals("Sản phẩm A", row1.getCell(1).getStringCellValue());
            assertEquals("Cái", row1.getCell(2).getStringCellValue());
            assertEquals("Gia dụng", row1.getCell(3).getStringCellValue());
            assertEquals(50000.0, row1.getCell(4).getNumericCellValue());
            assertEquals(12.0, row1.getCell(5).getNumericCellValue());
        }
    }

    @Test
    void testValidateImportAllOrNothing() throws IOException {
        File excelFile = tempDir.resolve("import_test.xlsx").toFile();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("HangHoa");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Mã hàng");
            header.createCell(1).setCellValue("Tên hàng hóa (*)");
            header.createCell(2).setCellValue("Đơn vị tính (*)");
            header.createCell(3).setCellValue("Danh mục");
            header.createCell(4).setCellValue("Giá vốn");
            header.createCell(5).setCellValue("Giá bán (*)");
            header.createCell(6).setCellValue("Tồn kho ban đầu");
            header.createCell(7).setCellValue("Ghi chú");

            // Dòng 1: Giá vốn âm -5000 (Lỗi)
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("SP01");
            r1.createCell(1).setCellValue("Hàng hóa 1");
            r1.createCell(2).setCellValue("Cái");
            r1.createCell(4).setCellValue("-5000");
            r1.createCell(5).setCellValue(10000);
            r1.createCell(6).setCellValue(10);

            // Dòng 2: Tồn kho âm -2 (Lỗi)
            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("SP02");
            r2.createCell(1).setCellValue("Hàng hóa 2");
            r2.createCell(2).setCellValue("Cái");
            r2.createCell(4).setCellValue(5000);
            r2.createCell(5).setCellValue(15000);
            r2.createCell(6).setCellValue("-2");

            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                workbook.write(fos);
            }
        }

        ImportExportService.ImportResult result = service.validateImport(excelFile);

        assertEquals(2, result.getTotalRows());
        assertEquals(2, result.getErrorRows());
        assertEquals(0, result.getValidRows());
        assertFalse(result.isAllValid()); // All-or-Nothing: Không được phép import

        ProductImportItem item1 = result.getAllRows().get(0);
        assertTrue(item1.isCostPriceError());
        assertTrue(item1.getErrorMessage().contains("Giá vốn"));

        ProductImportItem item2 = result.getAllRows().get(1);
        assertTrue(item2.isStockError());
        assertTrue(item2.getErrorMessage().contains("Tồn kho"));
    }

    @Test
    void testExecuteImportCreatesInventoryBatchAndMovement() throws SQLException {
        // Tạo trước 1 đơn vị tính hợp lệ
        int unitId;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO units (name) VALUES ('Hop')", java.sql.Statement.RETURN_GENERATED_KEYS)) {
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                assertTrue(rs.next());
                unitId = rs.getInt(1);
            }
        }

        List<ProductImportItem> items = new ArrayList<>();
        ProductImportItem item = new ProductImportItem();
        item.setRowNumber(2);
        item.setCode("SP_TEST_FIFO");
        item.setName("Sữa tươi tiệt trùng");
        item.setUnitId(unitId);
        item.setUnitName("Hop");
        item.setCostPrice(20000);
        item.setSalePrice(25000);
        item.setInitialStock(30);
        item.setNote("Nhập tồn ban đầu");
        items.add(item);

        service.executeImport(items);

        // Kiểm tra bảng products
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pStmt = conn.prepareStatement("SELECT id, code, stock_qty, sale_price FROM products WHERE code = 'SP_TEST_FIFO'")) {
            try (ResultSet rs = pStmt.executeQuery()) {
                assertTrue(rs.next());
                int productId = rs.getInt("id");
                assertEquals(30.0, rs.getDouble("stock_qty"));
                assertEquals(25000, rs.getLong("sale_price"));

                // Kiểm tra bảng inventory_batches
                try (PreparedStatement bStmt = conn.prepareStatement("SELECT cost_price, qty_initial, qty_remaining, source FROM inventory_batches WHERE product_id = ?")) {
                    bStmt.setInt(1, productId);
                    try (ResultSet bRs = bStmt.executeQuery()) {
                        assertTrue(bRs.next());
                        assertEquals(20000, bRs.getLong("cost_price"));
                        assertEquals(30, bRs.getInt("qty_initial"));
                        assertEquals(30, bRs.getInt("qty_remaining"));
                        assertEquals("INITIAL_STOCK", bRs.getString("source"));
                    }
                }

                // Kiểm tra bảng stock_movements
                try (PreparedStatement mStmt = conn.prepareStatement("SELECT type, qty_change, stock_after FROM stock_movements WHERE product_id = ?")) {
                    mStmt.setInt(1, productId);
                    try (ResultSet mRs = mStmt.executeQuery()) {
                        assertTrue(mRs.next());
                        assertEquals("TONKHO_BANDAU", mRs.getString("type"));
                        assertEquals(30.0, mRs.getDouble("qty_change"));
                        assertEquals(30.0, mRs.getDouble("stock_after"));
                    }
                }
            }
        }
    }
}
