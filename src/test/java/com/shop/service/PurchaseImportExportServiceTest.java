package com.shop.service;

import com.shop.infra.db.SchemaInitializer;
import com.shop.model.Purchase;
import com.shop.model.PurchaseItem;
import com.shop.util.DBConnection;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PurchaseImportExportServiceTest {

    private PurchaseImportExportService service;
    private PurchaseService purchaseService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws SQLException {
        System.setProperty("db.url", "jdbc:sqlite::memory:");
        SchemaInitializer.initialize();
        service = new PurchaseImportExportService();
        purchaseService = new PurchaseService();

        try (Connection conn = DBConnection.getConnection()) {
            // Tạo NCC001 và NCC002
            String insertSupplier = "INSERT INTO suppliers (code, name, phone, is_active, created_at) VALUES (?, ?, '0901234567', 1, datetime('now'))";
            try (PreparedStatement stmt = conn.prepareStatement(insertSupplier)) {
                stmt.setString(1, "NCC001");
                stmt.setString(2, "Công Ty Nước Giải Khát");
                stmt.executeUpdate();

                stmt.setString(1, "NCC002");
                stmt.setString(2, "Đại Lý Bánh Kẹo");
                stmt.executeUpdate();
            }

            // Tạo unit
            String insertUnit = "INSERT OR IGNORE INTO units (id, name, status) VALUES (1, 'Thùng', 1)";
            try (PreparedStatement stmt = conn.prepareStatement(insertUnit)) {
                stmt.executeUpdate();
            }

            // Tạo sản phẩm HH000001 và HH000002
            String insertProduct = "INSERT INTO products (code, name, unit_id, sale_price, stock_qty, created_at, updated_at) VALUES (?, ?, 1, 30000, 50.0, datetime('now'), datetime('now'))";
            try (PreparedStatement stmt = conn.prepareStatement(insertProduct)) {
                stmt.setString(1, "HH000001");
                stmt.setString(2, "Bia Tiger Lon");
                stmt.executeUpdate();

                stmt.setString(1, "HH000002");
                stmt.setString(2, "Nước Ngọt Coca");
                stmt.executeUpdate();
            }
        }
    }

    @Test
    void testGenerateTemplateAndValidateAndImportAndExport() throws IOException, SQLException {
        File templateFile = tempDir.resolve("mau-nhap-hang.xlsx").toFile();
        service.generateTemplate(templateFile);

        assertTrue(templateFile.exists());
        assertTrue(templateFile.length() > 0);

        // Kiểm tra cấu trúc sheet mẫu
        try (FileInputStream fis = new FileInputStream(templateFile);
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheet("PhieuNhap");
            assertNotNull(sheet);
            assertEquals("Mã phiếu (*)", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("Mã NCC (*)", sheet.getRow(0).getCell(1).getStringCellValue());
        }

        // Validate file mẫu (đã có sẵn NCC001, NCC002, HH000001, HH000002 trong DB)
        PurchaseImportExportService.PurchaseImportResult result = service.validateImportFile(templateFile);
        assertNotNull(result);
        assertEquals(3, result.getTotalRows(), "File mẫu có 3 dòng dữ liệu");
        assertEquals(3, result.getValidRows(), "Cả 3 dòng phải hợp lệ");
        assertEquals(0, result.getErrorRows(), "Không được có dòng lỗi");
        assertEquals(2, result.getTotalPurchases(), "Có 2 phiếu nhập riêng biệt (PN260901 có 2 dòng, PN260902 có 1 dòng)");
        assertTrue(result.isAllValid());

        // Thực thi Import
        service.executeImport(result.getAllRows());

        // Kiểm tra sau khi Import
        List<Purchase> allPurchases = purchaseService.getAllPurchases("", "Tất cả");
        assertEquals(2, allPurchases.size(), "Hệ thống phải có đúng 2 phiếu nhập được tạo");

        Purchase p1 = allPurchases.stream().filter(p -> "PN260901".equals(p.getCode())).findFirst().orElse(null);
        assertNotNull(p1);
        // Phiếu 1: Dòng 1 (20 * 25.000 = 500.000) + Dòng 2 (10 * 15.000 = 150.000) = 650.000
        assertEquals(650000, p1.getTotalCost());
        assertEquals(500000, p1.getPaid());
        assertEquals(150000, p1.getDebt());
        assertEquals("Còn nợ", p1.getStatus());

        List<PurchaseItem> p1Items = purchaseService.getItemsByPurchaseId(p1.getId());
        assertEquals(2, p1Items.size());

        // Kiểm tra tồn kho hàng HH000001: ban đầu 50 + p1(20) + p2(15) = 85
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT stock_qty FROM products WHERE code = 'HH000001'")) {
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(85.0, rs.getDouble("stock_qty"), 0.001);
            }
        }

        // Kiểm tra Export danh sách hàng loạt
        File exportListFile = tempDir.resolve("danh-sach-phieu-nhap.xlsx").toFile();
        service.exportPurchaseList(allPurchases, exportListFile);
        assertTrue(exportListFile.exists());
        assertTrue(exportListFile.length() > 0);

        // Kiểm tra Export chi tiết phiếu nhập
        File exportDetailFile = tempDir.resolve("chi-tiet-phieu-nhap.xlsx").toFile();
        service.exportPurchaseDetail(p1, p1Items, exportDetailFile);
        assertTrue(exportDetailFile.exists());
        assertTrue(exportDetailFile.length() > 0);
    }
}
