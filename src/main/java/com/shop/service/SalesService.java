package com.shop.service;

import com.shop.dao.*;
import com.shop.model.*;
import com.shop.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class SalesService {
    private InvoiceDao invoiceDao = new InvoiceDao();
    private InvoiceItemDao invoiceItemDao = new InvoiceItemDao();
    private ProductDao productDao = new ProductDao();
    private StockMovementDao stockMovementDao = new StockMovementDao();
    private ReturnInvoiceDao returnInvoiceDao = new ReturnInvoiceDao();
    private ReturnInvoiceItemDao returnInvoiceItemDao = new ReturnInvoiceItemDao();

    public void createInvoice(Invoice invoice, List<InvoiceItem> items) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            // 1. Insert invoice
            invoiceDao.insert(conn, invoice);

            // 2. Insert items and update stock
            String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            for (InvoiceItem item : items) {
                item.setInvoiceId(invoice.getId());
                
                // Set cost price
                com.shop.model.Product p = productDao.getById(conn, item.getProductId());
                if (p != null) {
                    item.setCostPrice(0); // MVP: cost price removed from products table
                } else {
                    item.setCostPrice(0);
                }
                
                // Update stock
                productDao.decreaseStock(conn, item.getProductId(), item.getQty());
                
                // Log movement
                StockMovement movement = new StockMovement();
                movement.setProductId(item.getProductId());
                movement.setType("OUT");
                movement.setQtyChange(-item.getQty());
                movement.setStockAfter(0); // Minimal impl
                movement.setRefType("INVOICE");
                movement.setRefId(invoice.getId());
                movement.setCreatedAt(currentTime);
                stockMovementDao.insert(conn, movement);
            }
            invoiceItemDao.insertAll(conn, items);

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

    public void createOrder(Invoice invoice, List<InvoiceItem> items) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);
            
            invoice.setStatus("PENDING");
            invoiceDao.insert(conn, invoice);

            for (InvoiceItem item : items) {
                item.setInvoiceId(invoice.getId());
                item.setCostPrice(0);
            }
            invoiceItemDao.insertAll(conn, items);
            
            // Do not decrease stock or log movement for PENDING orders in this MVP
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    public void processReturn(ReturnInvoice returnInvoice, List<ReturnInvoiceItem> items) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            if (returnInvoice.getReturnDate() == null) {
                returnInvoice.setReturnDate(currentTime);
            }
            returnInvoice.setStatus("COMPLETED");
            returnInvoiceDao.insert(conn, returnInvoice);

            for (ReturnInvoiceItem item : items) {
                item.setReturnInvoiceId(returnInvoice.getId());
                returnInvoiceItemDao.insert(conn, item);

                // Increase stock
                productDao.increaseStock(conn, item.getProductId(), item.getReturnQty());

                // Log movement
                StockMovement movement = new StockMovement();
                movement.setProductId(item.getProductId());
                movement.setType("RETURN");
                movement.setQtyChange(item.getReturnQty());
                movement.setStockAfter(0); // Minimal impl
                movement.setRefType("RETURN");
                movement.setRefId(returnInvoice.getId());
                movement.setCreatedAt(currentTime);
                stockMovementDao.insert(conn, movement);
            }
            
            // Cập nhật công nợ hóa đơn gốc (giảm nợ nếu có)
            long refundAmount = returnInvoice.getTotalRefund() - returnInvoice.getReturnFee();
            if (refundAmount > 0) {
                String query = "UPDATE invoices SET debt = MAX(0, debt - ?) WHERE id = ?";
                try (java.sql.PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setLong(1, refundAmount);
                    pstmt.setInt(2, returnInvoice.getInvoiceId());
                    pstmt.executeUpdate();
                }
            }
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }
}
