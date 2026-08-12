package com.shop.service;

import com.shop.dao.InvoiceDao;
import com.shop.dao.PaymentDao;
import com.shop.model.Payment;
import com.shop.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;

public class PaymentService {
    private PaymentDao paymentDao = new PaymentDao();
    private InvoiceDao invoiceDao = new InvoiceDao();

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
}
