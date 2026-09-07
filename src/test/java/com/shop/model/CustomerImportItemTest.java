package com.shop.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CustomerImportItemTest {

    @Test
    void testNoErrorByDefault() {
        CustomerImportItem item = new CustomerImportItem();
        item.setRowNumber(2);
        item.setCode("KH00001");
        item.setName("Nguyễn Văn A");
        item.setPhone("0987654321");
        
        assertFalse(item.hasError());
        assertEquals("", item.getErrorMessage());
    }

    @Test
    void testErrorAccumulation() {
        CustomerImportItem item = new CustomerImportItem();
        item.setRowNumber(3);
        item.setPhoneError(true);
        item.addErrorMessage("Số điện thoại không đúng 10 chữ số");
        item.setNameError(true);
        item.addErrorMessage("Tên khách hàng không được để trống");

        assertTrue(item.hasError());
        assertTrue(item.isPhoneError());
        assertTrue(item.isNameError());
        assertEquals("Số điện thoại không đúng 10 chữ số; Tên khách hàng không được để trống", item.getErrorMessage());
    }
}
