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
        assertEquals("27-06-2026 14:30", FormatterUtil.formatDateTime(dt));
        assertEquals("27-06-2026", FormatterUtil.formatDate(dt));
    }

    @Test
    void testFormatDateToDdMmYyyy() {
        assertEquals("10-09-2026", FormatterUtil.formatDateToDdMmYyyy("2026-09-10 21:30:26"));
        assertEquals("10-09-2026", FormatterUtil.formatDateToDdMmYyyy("2026-09-10"));
        assertEquals("10-09-2026", FormatterUtil.formatDateToDdMmYyyy("10/09/2026"));
        assertEquals("10-09-2026", FormatterUtil.formatDateToDdMmYyyy("10-09-2026"));
        assertEquals("12-08-2026", FormatterUtil.formatDateToDdMmYyyy("2026-08-12T15:21:57.250781500Z"));
        assertEquals("", FormatterUtil.formatDateToDdMmYyyy((String) null));
        assertEquals("", FormatterUtil.formatDateToDdMmYyyy("   "));

        java.time.LocalDate ld = java.time.LocalDate.of(2026, 9, 28);
        assertEquals("28-09-2026", FormatterUtil.formatDateToDdMmYyyy(ld));
        assertEquals("", FormatterUtil.formatDateToDdMmYyyy((java.time.LocalDate) null));

        java.time.LocalDateTime ldt = java.time.LocalDateTime.of(2026, 9, 28, 22, 30, 0);
        assertEquals("28-09-2026", FormatterUtil.formatDateToDdMmYyyy(ldt));
        assertEquals("", FormatterUtil.formatDateToDdMmYyyy((java.time.LocalDateTime) null));
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

    @Test
    void testFormatNumberWithDots() {
        assertEquals("0", FormatterUtil.formatNumberWithDots(0L));
        assertEquals("1.000", FormatterUtil.formatNumberWithDots(1000L));
        assertEquals("1.500.000", FormatterUtil.formatNumberWithDots(1500000L));
        assertEquals("25.000.000", FormatterUtil.formatNumberWithDots(25000000L));
    }

    @Test
    void testParseNumberFromText() {
        assertEquals(0L, FormatterUtil.parseNumberFromText(null));
        assertEquals(0L, FormatterUtil.parseNumberFromText(""));
        assertEquals(1500000L, FormatterUtil.parseNumberFromText("1.500.000"));
        assertEquals(1500000L, FormatterUtil.parseNumberFromText("1,500,000 đ"));
    }
}
