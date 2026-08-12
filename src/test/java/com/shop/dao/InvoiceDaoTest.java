package com.shop.dao;

import com.shop.model.Invoice;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.sql.Connection;

public class InvoiceDaoTest {
    @Test
    public void testInvoiceDao() {
        InvoiceDao dao = new InvoiceDao();
        assertNotNull(dao);
    }
}
