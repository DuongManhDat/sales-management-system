package com.shop.service;

import com.shop.dao.PriceHistoryDao;
import com.shop.dao.ProductDao;
import com.shop.model.PriceHistory;
import com.shop.model.Product;
import com.shop.util.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

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
                log.error("Lỗi khi thêm mới hàng hóa '{}': {}", product.getName(), e.getMessage(), e);
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
                log.error("Lỗi khi cập nhật hàng hóa ID={}: {}", product.getId(), e.getMessage(), e);
                throw e;
            }
        }
    }

    public void softDelete(int productId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            Product p = productDao.getById(conn, productId);
            if (p != null && p.getStockQty() > 0) {
                String errMsg = "Không thể ẩn hàng hóa vì vẫn còn tồn kho (" + p.getStockQty() + ").";
                log.warn("Cảnh báo khi xóa hàng hóa ID={}: {}", productId, errMsg);
                throw new IllegalStateException(errMsg);
            }
            productDao.setDeleted(conn, productId, true);
        } catch (SQLException e) {
            log.error("Lỗi SQL khi xóa hàng hóa ID={}: {}", productId, e.getMessage(), e);
            throw e;
        }
    }

    public void restore(int productId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            productDao.setDeleted(conn, productId, false);
        } catch (SQLException e) {
            log.error("Lỗi SQL khi khôi phục hàng hóa ID={}: {}", productId, e.getMessage(), e);
            throw e;
        }
    }

    public List<Product> getAllProducts() throws SQLException {
        try {
            return productDao.findAll();
        } catch (SQLException e) {
            log.error("Lỗi khi lấy danh sách hàng hóa: {}", e.getMessage(), e);
            throw e;
        }
    }

    public List<Product> searchActiveProducts(String keyword) throws SQLException {
        try {
            return productDao.searchProducts(keyword);
        } catch (SQLException e) {
            log.error("Lỗi khi tìm kiếm hàng hóa theo từ khóa '{}': {}", keyword, e.getMessage(), e);
            throw e;
        }
    }
    
    public Product getProductById(int productId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return productDao.getById(conn, productId);
        } catch (SQLException e) {
            log.error("Lỗi khi lấy thông tin hàng hóa ID={}: {}", productId, e.getMessage(), e);
            throw e;
        }
    }
    
    public List<PriceHistory> getPriceHistory(int productId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return priceHistoryDao.findByProduct(conn, productId);
        } catch (SQLException e) {
            log.error("Lỗi khi lấy lịch sử giá hàng hóa ID={}: {}", productId, e.getMessage(), e);
            throw e;
        }
    }
}
