package com.shop.service;

import com.shop.dao.UnitDao;
import com.shop.model.Unit;

import java.sql.SQLException;
import java.util.List;

public class UnitService {
    private final UnitDao unitDao;

    public UnitService() {
        this.unitDao = new UnitDao();
    }

    public List<Unit> findAllActive() {
        return unitDao.findAllActive();
    }

    public void add(String name) throws Exception {
        if (name == null || name.trim().isEmpty()) {
            throw new Exception("Tên đơn vị không được để trống.");
        }
        name = name.trim();
        for (Unit u : unitDao.findAllActive()) {
            if (u.getName().equalsIgnoreCase(name)) {
                throw new Exception("Tên đơn vị đã tồn tại, vui lòng chọn tên khác.");
            }
        }
        
        try {
            unitDao.add(name);
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                throw new Exception("Tên đơn vị đã tồn tại, vui lòng chọn tên khác.");
            }
            throw e;
        }
    }

    public void update(int id, String name) throws Exception {
        if (name == null || name.trim().isEmpty()) {
            throw new Exception("Tên đơn vị không được để trống.");
        }
        name = name.trim();
        for (Unit u : unitDao.findAllActive()) {
            if (u.getId() != id && u.getName().equalsIgnoreCase(name)) {
                throw new Exception("Tên đơn vị đã tồn tại, vui lòng chọn tên khác.");
            }
        }
        
        try {
            unitDao.update(id, name);
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                throw new Exception("Tên đơn vị đã tồn tại, vui lòng chọn tên khác.");
            }
            throw e;
        }
    }

    public void softDelete(int id) throws Exception {
        if (unitDao.countProductsUsingUnit(id) > 0) {
            throw new Exception("Không thể xóa. Đơn vị đang được sử dụng bởi sản phẩm.");
        }
        unitDao.softDelete(id);
    }
}
