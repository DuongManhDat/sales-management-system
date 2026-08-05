package com.shop.service;

import com.shop.dao.CategoryDao;
import com.shop.model.Category;

import java.sql.SQLException;
import java.util.List;

public class CategoryService {
    private final CategoryDao categoryDao;

    public CategoryService() {
        this.categoryDao = new CategoryDao();
    }

    public List<Category> findAllActive() {
        return categoryDao.findAllActive();
    }

    public void add(String name) throws Exception {
        if (name == null || name.trim().isEmpty()) {
            throw new Exception("Tên nhóm hàng không được để trống.");
        }
        name = name.trim();
        for (Category c : categoryDao.findAllActive()) {
            if (c.getName().equalsIgnoreCase(name)) {
                throw new Exception("Tên nhóm hàng đã tồn tại, vui lòng chọn tên khác.");
            }
        }
        
        try {
            categoryDao.add(name);
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                throw new Exception("Tên nhóm hàng đã tồn tại, vui lòng chọn tên khác.");
            }
            throw e;
        }
    }

    public void update(int id, String name) throws Exception {
        if (name == null || name.trim().isEmpty()) {
            throw new Exception("Tên nhóm hàng không được để trống.");
        }
        name = name.trim();
        for (Category c : categoryDao.findAllActive()) {
            if (c.getId() != id && c.getName().equalsIgnoreCase(name)) {
                throw new Exception("Tên nhóm hàng đã tồn tại, vui lòng chọn tên khác.");
            }
        }
        
        try {
            categoryDao.update(id, name);
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                throw new Exception("Tên nhóm hàng đã tồn tại, vui lòng chọn tên khác.");
            }
            throw e;
        }
    }

    public void softDelete(int id) throws Exception {
        if (categoryDao.countProductsUsingCategory(id) > 0) {
            throw new Exception("Không thể xóa. Nhóm hàng đang được sử dụng bởi sản phẩm.");
        }
        categoryDao.softDelete(id);
    }
}
