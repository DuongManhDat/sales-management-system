package com.shop.service;

import com.shop.dao.InvoiceDao;
import com.shop.dao.PaymentDao;
import com.shop.model.Invoice;
import com.shop.model.Payment;
import com.shop.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PaymentService {
    private final PaymentDao paymentDao = new PaymentDao();
    private final InvoiceDao invoiceDao = new InvoiceDao();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void processPayment(Payment payment, long newPaid, long newDebt, String newStatus) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            paymentDao.insert(conn, payment);
            invoiceDao.updatePaymentStatus(conn, payment.getInvoiceId(), newPaid, newDebt, newStatus);

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    public Invoice payInvoiceDebt(int invoiceId, long amount, String note) throws SQLException {
        Invoice current = invoiceDao.findById(invoiceId);
        if (current == null) {
            throw new IllegalArgumentException("Hóa đơn #" + invoiceId + " không tồn tại.");
        }

        if (current.getDebt() <= 0) {
            throw new IllegalStateException("Hóa đơn " + current.getCode() + " đã được thanh toán đủ, không còn nợ.");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền thanh toán phải lớn hơn 0.");
        }

        if (amount > current.getDebt()) {
            throw new IllegalArgumentException("Số tiền thanh toán (" + amount + " đ) không được vượt quá số tiền còn nợ (" + current.getDebt() + " đ).");
        }

        long newDebt = current.getDebt() - amount;
        long newPaid = current.getPaid() + amount;
        String newStatus = (newDebt == 0) ? "Đã thanh toán" : "Còn nợ";
        String paymentDate = LocalDateTime.now().format(DATE_TIME_FORMATTER);

        Payment payment = new Payment();
        payment.setInvoiceId(invoiceId);
        payment.setAmount(amount);
        payment.setPaymentDate(paymentDate);
        payment.setNote(note != null ? note : "Khách hàng thanh toán nợ");

        processPayment(payment, newPaid, newDebt, newStatus);

        current.setPaid(newPaid);
        current.setDebt(newDebt);
        current.setStatus(newStatus);
        return current;
    }

    public Invoice cancelPayment(int paymentId, String reason) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            Payment payment = paymentDao.findById(conn, paymentId);
            if (payment == null) {
                throw new IllegalArgumentException("Đợt thanh toán #" + paymentId + " không tồn tại.");
            }

            if ("CANCELLED".equalsIgnoreCase(payment.getStatus())) {
                throw new IllegalStateException("Đợt thanh toán #" + paymentId + " đã được hủy trước đó.");
            }

            Invoice invoice = invoiceDao.findById(conn, payment.getInvoiceId());
            if (invoice == null) {
                throw new IllegalArgumentException("Hóa đơn #" + payment.getInvoiceId() + " không tồn tại.");
            }

            long newPaid = invoice.getPaid() - payment.getAmount();
            if (newPaid < 0) newPaid = 0;
            long newDebt = invoice.getDebt() + payment.getAmount();
            String newStatus = (newDebt > 0) ? "Còn nợ" : "Đã thanh toán";
            String cancelledAt = LocalDateTime.now().format(DATE_TIME_FORMATTER);
            String actualReason = (reason != null && !reason.trim().isEmpty()) ? reason.trim() : "Hủy do thao tác nhầm";

            // Đánh dấu hủy và lưu vết lý do, thời điểm hủy (Audit Log)
            paymentDao.markCancelled(conn, paymentId, cancelledAt, actualReason);

            // Cập nhật lại số nợ hóa đơn
            invoiceDao.updatePaymentStatus(conn, invoice.getId(), newPaid, newDebt, newStatus);

            conn.commit();

            invoice.setPaid(newPaid);
            invoice.setDebt(newDebt);
            invoice.setStatus(newStatus);
            return invoice;
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }
}
