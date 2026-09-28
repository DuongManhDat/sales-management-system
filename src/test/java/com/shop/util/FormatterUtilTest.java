package com.shop.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

class FormatterUtilTest {

    @Test
    void testFormatCurrency() {
        assertEquals("1.000.000\u00a0\u20ab", FormatterUtil.formatCurrency(new BigDecimal("1000000")).replace(" ", "\u00a0"));
    }

    @Test
    void testParseCurrency() {
        assertEquals(new BigDecimal("1500000"), FormatterUtil.parseCurrency("1.500.000 đ"));
        assertEquals(new BigDecimal("50000"), FormatterUtil.parseCurrency("50.000"));
        assertEquals(new BigDecimal("0"), FormatterUtil.parseCurrency("abc"));
        assertEquals(new BigDecimal("0"), FormatterUtil.parseCurrency(""));
        assertEquals(new BigDecimal("0"), FormatterUtil.parseCurrency(null));
    }

    @Test
    void testFormatDate() {
        LocalDateTime dt = LocalDateTime.of(2026, 6, 27, 14, 30);
        assertEquals("27/06/2026 14:30", FormatterUtil.formatDateTime(dt));
        assertEquals("27/06/2026", FormatterUtil.formatDate(dt));
    }

    @Test
    void testFormatDateToDdMmYyyy() {
        assertEquals("10-09-2026", FormatterUtil.formatDateToDdMmYyyy("2026-09-10 21:30:26"));
        assertEquals("10-09-2026", FormatterUtil.formatDateToDdMmYyyy("2026-09-10"));
        assertEquals("10-09-2026", FormatterUtil.formatDateToDdMmYyyy("10/09/2026"));
        assertEquals("10-09-2026", FormatterUtil.formatDateToDdMmYyyy("10-09-2026"));
        assertEquals("", FormatterUtil.formatDateToDdMmYyyy(null));
        assertEquals("", FormatterUtil.formatDateToDdMmYyyy("   "));
    }

    @Test
    void testFormatPurchaseStatus() {
        assertEquals("Đã thanh toán", FormatterUtil.formatPurchaseStatus("PAID"));
        assertEquals("Đã thanh toán", FormatterUtil.formatPurchaseStatus("Đã thanh toán"));
        assertEquals("Đã thanh toán", FormatterUtil.formatPurchaseStatus("COMPLETED"));
        assertEquals("Còn nợ", FormatterUtil.formatPurchaseStatus("DEBT"));
        assertEquals("Còn nợ", FormatterUtil.formatPurchaseStatus("UNPAID"));
        assertEquals("Còn nợ", FormatterUtil.formatPurchaseStatus("Còn nợ"));
    }

    @Test
    void testFormatInvoiceStatus() {
        assertEquals("Đã thanh toán", FormatterUtil.formatInvoiceStatus("PAID"));
        assertEquals("Đã thanh toán", FormatterUtil.formatInvoiceStatus("Đã thanh toán"));
        assertEquals("Đã thanh toán", FormatterUtil.formatInvoiceStatus("ĐÃ THANH TOÁN"));
        assertEquals("Còn nợ", FormatterUtil.formatInvoiceStatus("DEBT"));
        assertEquals("Còn nợ", FormatterUtil.formatInvoiceStatus("UNPAID"));
        assertEquals("Còn nợ", FormatterUtil.formatInvoiceStatus("PARTIAL"));
        assertEquals("Còn nợ", FormatterUtil.formatInvoiceStatus("Còn nợ"));
        assertEquals("Đang xử lý", FormatterUtil.formatInvoiceStatus("PENDING"));
        assertEquals("Đang xử lý", FormatterUtil.formatInvoiceStatus("Đang xử lý"));
        assertEquals("Đã hủy", FormatterUtil.formatInvoiceStatus("CANCELLED"));
        assertEquals("Đã hủy", FormatterUtil.formatInvoiceStatus("CANCELED"));
        assertEquals("Đã hủy", FormatterUtil.formatInvoiceStatus("Đã hủy"));
        assertEquals("Đã hoàn thành", FormatterUtil.formatInvoiceStatus("COMPLETED"));
        assertEquals("Đã hoàn thành", FormatterUtil.formatInvoiceStatus("Đã hoàn thành"));
        assertEquals("Đã thanh toán", FormatterUtil.formatInvoiceStatus((String) null));
        assertEquals("Đã thanh toán", FormatterUtil.formatInvoiceStatus("   "));
    }

    @Test
    void testFormatInvoiceStatusWithInvoice() {
        com.shop.model.Invoice inv1 = new com.shop.model.Invoice();
        inv1.setStatus("PAID");
        assertEquals("Đã thanh toán", FormatterUtil.formatInvoiceStatus(inv1));

        com.shop.model.Invoice inv2 = new com.shop.model.Invoice();
        inv2.setStatus("DEBT");
        assertEquals("Còn nợ", FormatterUtil.formatInvoiceStatus(inv2));

        com.shop.model.Invoice inv3 = new com.shop.model.Invoice();
        inv3.setStatus(null);
        inv3.setDebt(150000L);
        assertEquals("Còn nợ", FormatterUtil.formatInvoiceStatus(inv3));

        com.shop.model.Invoice inv4 = new com.shop.model.Invoice();
        inv4.setStatus(null);
        inv4.setDebt(0L);
        assertEquals("Đã thanh toán", FormatterUtil.formatInvoiceStatus(inv4));

        assertEquals("", FormatterUtil.formatInvoiceStatus((com.shop.model.Invoice) null));
    }
}
