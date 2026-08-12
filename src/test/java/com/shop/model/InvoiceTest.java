package com.shop.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class InvoiceTest {
    @Test
    public void testInvoiceDebtCalculation() {
        Invoice invoice = new Invoice();
        invoice.setTotal(100000L);
        invoice.setPaid(40000L);
        assertEquals(60000L, invoice.getTotal() - invoice.getPaid());
    }
}
