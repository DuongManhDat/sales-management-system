package com.shop.service;

import com.shop.dao.PriceHistoryDao;
import com.shop.dao.ProductDao;
import com.shop.model.PriceHistory;
import com.shop.model.Product;
import com.shop.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class ProductService {
    private final ProductDao productDao = new ProductDao();
    private final PriceHistoryDao priceHistoryDao = new PriceHistoryDao();

    public int addProduct(Product product) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int id = productDao.insert(conn, product);
                conn.commit();
                return id;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void updateProduct(Product product) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Product oldProduct = productDao.getById(conn, product.getId());
                if (oldProduct != null && oldProduct.getSalePrice() != product.getSalePrice()) {
                    PriceHistory history = new PriceHistory();
                    history.setProductId(product.getId());
                    history.setOldPrice(oldProduct.getSalePrice());
                    history.setNewPrice(product.getSalePrice());
                    priceHistoryDao.insert(conn, history);
                }
                
                productDao.update(conn, product);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void softDelete(int productId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            Product p = productDao.getById(conn, productId);
            if (p != null && p.getStockQty() > 0) {
                throw new IllegalStateException("Không thể ẩn hàng hóa vì vẫn còn tồn kho (" + p.getStockQty() + ").");
            }
            productDao.setDeleted(conn, productId, true);
        }
    }

    public void restore(int productId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            productDao.setDeleted(conn, productId, false);
        }
    }

    public List<Product> getAllProducts() throws SQLException {
        return productDao.findAll();
    }

    public List<Product> searchActiveProducts(String keyword) throws SQLException {
        return productDao.searchProducts(keyword);
    }
    
    public Product getProductById(int productId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return productDao.getById(conn, productId);
        }
    }
    
    public List<PriceHistory> getPriceHistory(int productId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return priceHistoryDao.findByProduct(conn, productId);
        }
    }
}
