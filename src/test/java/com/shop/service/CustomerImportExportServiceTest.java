package com.shop.service;

import com.shop.model.Customer;
import com.shop.model.Gender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CustomerImportExportServiceTest {

    private CustomerImportExportService service;

    @BeforeEach
    void setUp() {
        service = new CustomerImportExportService();
    }

    @Test
    void testGenerateTemplateAndValidate(@TempDir Path tempDir) throws Exception {
        File templateFile = tempDir.resolve("mau-nhap-khach-hang.xlsx").toFile();
        service.generateTemplate(templateFile);
        assertTrue(templateFile.exists());
        assertTrue(templateFile.length() > 0);

        // Validate template file (chứa 2 dòng dữ liệu mẫu)
        CustomerImportExportService.CustomerImportResult result = service.validateImport(templateFile);
        assertNotNull(result);
        assertEquals(2, result.getTotalRows());
        assertEquals(2, result.getAllRows().size());
        assertEquals("Nguyễn Văn An", result.getAllRows().get(0).getName());
        assertEquals("0912345678", result.getAllRows().get(0).getPhone());
        assertEquals(Gender.MALE, result.getAllRows().get(0).getGender());
    }

    @Test
    void testExportCustomers(@TempDir Path tempDir) throws Exception {
        File exportFile = tempDir.resolve("danh-sach-khach-hang.xlsx").toFile();
        List<Customer> list = new ArrayList<>();
        Customer c1 = new Customer();
        c1.setCode("KH00001");
        c1.setName("Lê Văn C");
        c1.setPhone("0933111222");
        c1.setEmail("levanc@gmail.com");
        c1.setDateOfBirth(LocalDate.of(1992, 6, 20));
        c1.setGender(Gender.MALE);
        c1.setAddress("Hà Nội");
        c1.setActive(true);
        list.add(c1);

        service.exportCustomers(exportFile, list);
        assertTrue(exportFile.exists());
        assertTrue(exportFile.length() > 0);
    }
}
