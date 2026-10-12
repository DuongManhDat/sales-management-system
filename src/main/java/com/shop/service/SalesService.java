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
    private PaymentDao paymentDao = new PaymentDao();

    public void createInvoice(Invoice invoice, List<InvoiceItem> items) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction
            String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            if (invoice.getCode() == null || invoice.getCode().trim().isEmpty()) {
                invoice.setCode("HD" + new SimpleDateFormat("yyMMddHHmmss").format(new Date()));
            }
            if (invoice.getInvoiceDate() == null || invoice.getInvoiceDate().trim().isEmpty()) {
                invoice.setInvoiceDate(currentTime);
            }

            // 1. Insert invoice
            invoiceDao.insert(conn, invoice);
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
        createOrder(invoice, items, 0L);
    }

    public void createOrder(Invoice invoice, List<InvoiceItem> items, long depositAmount) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            if (invoice.getCode() == null || invoice.getCode().trim().isEmpty()) {
                invoice.setCode("DH" + new SimpleDateFormat("yyMMddHHmmss").format(new Date()) + String.format("%03d", (int)(System.currentTimeMillis() % 1000)));
            }
            if (invoice.getInvoiceDate() == null || invoice.getInvoiceDate().trim().isEmpty()) {
                invoice.setInvoiceDate(currentTime);
            }

            long paid = Math.max(0, depositAmount);
            invoice.setPaid(paid);
            invoice.setDebt(invoice.getTotal() - paid);
            invoice.setStatus("PENDING");

            // Kiểm tra tồn kho khả dụng TRƯỚC khi lưu
            for (InvoiceItem item : items) {
                double available = productDao.getAvailableQty(conn, item.getProductId());
                if (item.getQty() > available) {
                    Product p = productDao.getById(conn, item.getProductId());
                    String name = p != null ? p.getName() : "Sản phẩm #" + item.getProductId();
                    throw new IllegalStateException("Sản phẩm \"" + name + "\" không đủ tồn kho khả dụng. " +
                            "Cần: " + item.getQty() + ", Có thể đặt: " + (int) available);
                }
            }

            invoiceDao.insert(conn, invoice);

            for (InvoiceItem item : items) {
                item.setInvoiceId(invoice.getId());
                item.setCostPrice(0);
                productDao.increaseReservedQty(conn, item.getProductId(), item.getQty());
            }
            invoiceItemDao.insertAll(conn, items);

            if (depositAmount > 0) {
                Payment deposit = new Payment();
                deposit.setInvoiceId(invoice.getId());
                deposit.setAmount(depositAmount);
                deposit.setPaymentDate(currentTime);
                deposit.setNote("Tiền cọc đặt hàng " + invoice.getCode());
                deposit.setPaymentType("DEPOSIT");
                paymentDao.insert(conn, deposit);
            }

            conn.commit();
        } catch (SQLException | IllegalStateException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    public void completeOrder(int invoiceId) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            Invoice invoice = invoiceDao.findById(conn, invoiceId);
            if (invoice == null) throw new IllegalArgumentException("Đơn hàng #" + invoiceId + " không tồn tại.");
            if (!"PENDING".equals(invoice.getStatus()))
                throw new IllegalArgumentException("Chỉ hoàn thành đơn PENDING. Trạng thái hiện tại: " + invoice.getStatus());

            List<InvoiceItem> items = invoiceItemDao.findByInvoiceId(conn, invoiceId);
            String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            for (InvoiceItem item : items) {
                productDao.decreaseStock(conn, item.getProductId(), item.getQty());
                productDao.decreaseReservedQty(conn, item.getProductId(), item.getQty());
                StockMovement movement = new StockMovement();
                movement.setProductId(item.getProductId());
                movement.setType("OUT");
                movement.setQtyChange(-item.getQty());
                movement.setStockAfter(0);
                movement.setRefType("ORDER");
                movement.setRefId(invoiceId);
                movement.setCreatedAt(currentTime);
                stockMovementDao.insert(conn, movement);
            }

            invoiceDao.updatePaymentStatus(conn, invoiceId, invoice.getPaid(), invoice.getDebt(), "COMPLETED");
            conn.commit();
        } catch (SQLException | IllegalArgumentException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    public void cancelOrder(int invoiceId) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            Invoice invoice = invoiceDao.findById(conn, invoiceId);
            if (invoice == null) throw new IllegalArgumentException("Đơn hàng #" + invoiceId + " không tồn tại.");
            if (!"PENDING".equals(invoice.getStatus()))
                throw new IllegalArgumentException("Chỉ hủy đơn PENDING. Trạng thái hiện tại: " + invoice.getStatus());

            List<InvoiceItem> items = invoiceItemDao.findByInvoiceId(conn, invoiceId);
            for (InvoiceItem item : items) {
                productDao.decreaseReservedQty(conn, item.getProductId(), item.getQty());
            }

            invoiceDao.updatePaymentStatus(conn, invoiceId, invoice.getPaid(), invoice.getDebt(), "CANCELLED");
            conn.commit();
        } catch (SQLException | IllegalArgumentException e) {
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
