package com.shop.service;

import com.shop.dao.ProductDao;
import com.shop.dao.StockAdjustmentDao;
import com.shop.infra.db.SchemaInitializer;
import com.shop.model.Product;
import com.shop.model.StockAdjustment;
import com.shop.model.StockAdjustmentImportItem;
import com.shop.model.StockAdjustmentItem;
import com.shop.util.DBConnection;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StockAdjustmentImportExportServiceTest {

    private StockAdjustmentImportExportService service;
    private ProductDao productDao;
    private StockAdjustmentDao stockAdjustmentDao;
    private AdjustmentService adjustmentService;

    private int productId1;
    private int productId2;

    @BeforeEach
    void setUp() throws SQLException {
        System.setProperty("db.url", "jdbc:sqlite::memory:");
        DBConnection.close();
        SchemaInitializer.initialize();

        service = new StockAdjustmentImportExportService();
        productDao = new ProductDao();
        stockAdjustmentDao = new StockAdjustmentDao();
        adjustmentService = new AdjustmentService();

        try (Connection conn = DBConnection.getConnection()) {
            int unitId;
            try (PreparedStatement pUnit = conn.prepareStatement("INSERT INTO units (name) VALUES ('Cái')", Statement.RETURN_GENERATED_KEYS)) {
                pUnit.executeUpdate();
                try (ResultSet rs = pUnit.getGeneratedKeys()) {
                    rs.next();
                    unitId = rs.getInt(1);
                }
            }

            Product p1 = new Product();
            p1.setCode("SP01");
            p1.setName("Sản phẩm 01");
            p1.setStockQty(10.0);
            p1.setSalePrice(50000L);
            p1.setUnitId(unitId);
            productId1 = productDao.insert(conn, p1);
            productDao.updateCode(conn, productId1, "SP01");

            Product p2 = new Product();
            p2.setCode("SP02");
            p2.setName("Sản phẩm 02");
            p2.setStockQty(20.0);
            p2.setSalePrice(100000L);
            p2.setUnitId(unitId);
            productId2 = productDao.insert(conn, p2);
            productDao.updateCode(conn, productId2, "SP02");
        }
    }

    @Test
    void testGenerateTemplate(@TempDir Path tempDir) throws Exception {
        File file = tempDir.resolve("mau_kiem_kho.xlsx").toFile();
        service.generateTemplate(file);

        assertTrue(file.exists());
        assertTrue(file.length() > 0);

        try (Workbook wb = new XSSFWorkbook(file)) {
            assertEquals(2, wb.getNumberOfSheets());
            assertEquals("Danh sách hàng hóa", wb.getSheetAt(0).getSheetName());
            assertEquals("Kiểm kho", wb.getSheetAt(1).getSheetName());

            Sheet sheet1 = wb.getSheetAt(0);
            assertTrue(sheet1.getLastRowNum() >= 2); // Header + 2 sản phẩm
        }
    }

    @Test
    void testValidateImportFile_ValidAndDuplicateRules(@TempDir Path tempDir) throws IOException, SQLException {
        File file = tempDir.resolve("import_test.xlsx").toFile();

        try (Workbook wb = new XSSFWorkbook()) {
            wb.createSheet("Danh_Muc_Hang_Hoa");
            Sheet sheet = wb.createSheet("Kiem_Kho");

            Row h = sheet.createRow(0);
            h.createCell(0).setCellValue("Mã hàng (*)");
            h.createCell(1).setCellValue("Tên hàng hóa");
            h.createCell(2).setCellValue("Tồn thực tế (*)");
            h.createCell(3).setCellValue("Lý do");

            // Row 1: SP01 hợp lệ
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("SP01");
            r1.createCell(1).setCellValue("Sản phẩm 01");
            r1.createCell(2).setCellValue(12.0);
            r1.createCell(3).setCellValue("Kiểm đếm");

            // Row 2: SP_NOT_EXIST lỗi không tồn tại
            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("SP_UNKNOWN");
            r2.createCell(2).setCellValue(5.0);

            // Row 3 & 4: SP02 lặp lại với số lượng khác nhau (20 và 25) -> báo lỗi trùng
            Row r3 = sheet.createRow(3);
            r3.createCell(0).setCellValue("SP02");
            r3.createCell(2).setCellValue(20.0);

            Row r4 = sheet.createRow(4);
            r4.createCell(0).setCellValue("SP02");
            r4.createCell(2).setCellValue(25.0);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }

        StockAdjustmentImportExportService.ImportResult result = service.validateImportFile(file);
        for (StockAdjustmentImportItem it : result.getAllRows()) {
            System.out.println("Row " + it.getRowNumber() + " [" + it.getProductCode() + "]: hasError=" + it.hasError() + ", msg=" + it.getErrorMessage());
        }
        assertEquals(4, result.getTotalRows());
        assertEquals(1, result.getValidRows()); // Chỉ có SP01 hợp lệ
        assertEquals(3, result.getErrorRows()); // r2, r3, r4 lỗi
    }

    @Test
    void testValidateImportFile_CrossAdjustmentError(@TempDir Path tempDir) throws IOException, SQLException {
        // Tạo trước 1 phiếu kiểm kho hoàn tất hôm nay với SP01 thực tế = 15
        StockAdjustment adj = new StockAdjustment();
        adj.setNote("Phiếu sáng");
        List<StockAdjustmentItem> items = new ArrayList<>();
        StockAdjustmentItem it = new StockAdjustmentItem();
        it.setProductId(productId1);
        it.setCurrentQty(10.0);
        it.setActualQty(15.0);
        it.setVariance(5.0);
        it.setCostPrice(50000L);
        items.add(it);
        adjustmentService.createAdjustment(adj, items);

        // Tạo file import có SP01 nhưng số lượng thực tế = 12 (khác 15 đã kiểm trước đó)
        File file = tempDir.resolve("import_cross.xlsx").toFile();
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Kiem_Kho");
            Row h = sheet.createRow(0);
            h.createCell(0).setCellValue("Mã hàng");
            h.createCell(2).setCellValue("Tồn thực tế");

            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("SP01");
            r1.createCell(2).setCellValue(12.0);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }

        StockAdjustmentImportExportService.ImportResult result = service.validateImportFile(file);
        assertEquals(1, result.getTotalRows());
        assertEquals(0, result.getValidRows());
        assertEquals(1, result.getErrorRows());
        assertTrue(result.getAllRows().get(0).isCrossAdjustmentError());
    }
}
