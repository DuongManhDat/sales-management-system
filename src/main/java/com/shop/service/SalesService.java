package com.shop.service;

import com.shop.dao.InvoiceDao;
import com.shop.dao.InvoiceItemDao;
import com.shop.dao.ProductDao;
import com.shop.dao.StockMovementDao;
import com.shop.model.Invoice;
import com.shop.model.InvoiceItem;
import com.shop.model.StockMovement;
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
}
