package com.shop.service;

import com.shop.infra.db.SchemaInitializer;
import com.shop.model.Customer;
import com.shop.model.Invoice;
import com.shop.model.Payment;
import com.shop.dao.CustomerDao;
import com.shop.dao.InvoiceDao;
import com.shop.dao.PaymentDao;
import com.shop.util.DBConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaymentServiceTest {

    private PaymentService paymentService;
    private InvoiceDao invoiceDao;
    private CustomerDao customerDao;
    private PaymentDao paymentDao;
    private int testInvoiceId;

    @BeforeEach
    void setUp() throws SQLException {
        System.setProperty("db.url", "jdbc:sqlite::memory:");
        SchemaInitializer.initialize();

        paymentService = new PaymentService();
        invoiceDao = new InvoiceDao();
        customerDao = new CustomerDao();
        paymentDao = new PaymentDao();

        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement del0 = conn.prepareStatement("DELETE FROM invoice_item_batches");
                 PreparedStatement del1 = conn.prepareStatement("DELETE FROM payments");
                 PreparedStatement del2 = conn.prepareStatement("DELETE FROM invoice_items");
                 PreparedStatement del3 = conn.prepareStatement("DELETE FROM invoices");
                 PreparedStatement del4 = conn.prepareStatement("DELETE FROM customers")) {
                del0.executeUpdate();
                del1.executeUpdate();
                del2.executeUpdate();
                del3.executeUpdate();
                del4.executeUpdate();
            }

            Customer c = new Customer();
            c.setCode("KH0001");
            c.setName("Nguyễn Văn A");
            c.setPhone("0912345678");
            customerDao.insert(conn, c);

            Invoice inv = new Invoice();
            inv.setCode("HD0001");
            inv.setCustomerId(c.getId());
            inv.setInvoiceDate("2026-10-01 10:00:00");
            inv.setSubtotal(100000);
            inv.setDiscountPct(0);
            inv.setDiscountAmt(0);
            inv.setTotal(100000);
            inv.setPaid(30000);
            inv.setDebt(70000);
            inv.setStatus("Còn nợ");
            invoiceDao.insert(conn, inv);

            testInvoiceId = inv.getId();
        }
    }

    @Test
    void testPayInvoiceDebt_PartialPayment_Success() throws SQLException {
        Invoice updated = paymentService.payInvoiceDebt(testInvoiceId, 30000, "Trả nợ đợt 1");

        assertNotNull(updated);
        assertEquals(60000, updated.getPaid());
        assertEquals(40000, updated.getDebt());
        assertEquals("Còn nợ", updated.getStatus());

        List<Payment> payments = paymentDao.findByInvoiceId(testInvoiceId);
        assertEquals(1, payments.size());
        assertEquals(30000, payments.get(0).getAmount());
        assertEquals("Trả nợ đợt 1", payments.get(0).getNote());
    }

    @Test
    void testPayInvoiceDebt_FullPayment_Success() throws SQLException {
        Invoice updated = paymentService.payInvoiceDebt(testInvoiceId, 70000, "Trả hết nợ còn lại");

        assertNotNull(updated);
        assertEquals(100000, updated.getPaid());
        assertEquals(0, updated.getDebt());
        assertEquals("Đã thanh toán", updated.getStatus());

        List<Payment> payments = paymentDao.findByInvoiceId(testInvoiceId);
        assertEquals(1, payments.size());
        assertEquals(70000, payments.get(0).getAmount());
    }

    @Test
    void testPayInvoiceDebt_ExceedDebt_ThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            paymentService.payInvoiceDebt(testInvoiceId, 75000, "Trả lố nợ");
        });
        assertTrue(ex.getMessage().contains("không được vượt quá số tiền còn nợ"));
    }

    @Test
    void testPayInvoiceDebt_ZeroOrNegative_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            paymentService.payInvoiceDebt(testInvoiceId, 0, "Không có tiền");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            paymentService.payInvoiceDebt(testInvoiceId, -10000, "Số âm");
        });
    }

    @Test
    void testPayInvoiceDebt_AlreadyPaid_ThrowsException() throws SQLException {
        paymentService.payInvoiceDebt(testInvoiceId, 70000, "Trả hết");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            paymentService.payInvoiceDebt(testInvoiceId, 10000, "Trả thêm sau khi đã hết nợ");
        });
        assertTrue(ex.getMessage().contains("đã được thanh toán đủ"));
    }

    @Test
    void testCancelPayment_Success() throws SQLException {
        paymentService.payInvoiceDebt(testInvoiceId, 50000, "Trả nhầm 50k");
        List<Payment> list = paymentDao.findByInvoiceId(testInvoiceId);
        assertEquals(1, list.size());
        int paymentId = list.get(0).getId();

        Invoice canceledInvoice = paymentService.cancelPayment(paymentId, "Thu nhầm tiền mặt");
        assertNotNull(canceledInvoice);
        assertEquals(30000, canceledInvoice.getPaid());
        assertEquals(70000, canceledInvoice.getDebt());
        assertEquals("Còn nợ", canceledInvoice.getStatus());

        List<Payment> afterCancel = paymentDao.findByInvoiceId(testInvoiceId);
        assertEquals(1, afterCancel.size());
        Payment canceledPayment = afterCancel.get(0);
        assertEquals("CANCELLED", canceledPayment.getStatus());
        assertEquals("Thu nhầm tiền mặt", canceledPayment.getCancelReason());
        assertNotNull(canceledPayment.getCancelledAt());

        // Kiểm tra không cho phép hủy 2 lần
        assertThrows(IllegalStateException.class, () -> {
            paymentService.cancelPayment(paymentId, "Hủy lần 2");
        });
    }
}
