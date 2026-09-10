package com.shop.service;

import com.shop.dao.SupplierDao;
import com.shop.model.Supplier;
import javafx.concurrent.Task;

import java.util.List;

public class SupplierService {
    private final SupplierDao supplierDao;

    public SupplierService() {
        this.supplierDao = new SupplierDao();
    }

    public List<Supplier> findAllActive() throws java.sql.SQLException {
        return supplierDao.findAllActive();
    }

    public Task<List<Supplier>> getAllActiveSuppliersTask() {
        return new Task<List<Supplier>>() {
            @Override
            protected List<Supplier> call() throws Exception {
                return supplierDao.findAllActive();
            }
        };
    }

    public Task<Void> saveSupplierTask(Supplier supplier) {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // Validation
                if (supplier.getName() == null || supplier.getName().trim().isEmpty()) {
                    throw new IllegalArgumentException("Tên nhà cung cấp không được để trống.");
                }
                if (supplier.getPhone() == null || supplier.getPhone().trim().isEmpty()) {
                    throw new IllegalArgumentException("Số điện thoại không được để trống.");
                }

                // Unique validation
                int excludeId = supplier.getId();
                if (supplier.getCode() != null && !supplier.getCode().trim().isEmpty()) {
                    if (supplierDao.existsByCode(supplier.getCode().trim(), excludeId)) {
                        throw new IllegalArgumentException("Mã nhà cung cấp đã tồn tại.");
                    }
                }
                if (supplierDao.existsByPhone(supplier.getPhone().trim(), excludeId)) {
                    throw new IllegalArgumentException("Số điện thoại đã tồn tại.");
                }

                if (supplier.getId() == 0) {
                    supplierDao.insert(supplier);
                } else {
                    supplierDao.update(supplier);
                }
                return null;
            }
        };
    }

    public Task<Void> deleteSupplierTask(int supplierId) {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // In phase 2, we just soft delete it. If it's used in purchases, 
                // we might want to check here, but the spec says "Xóa mềm".
                supplierDao.setActive(supplierId, false);
                return null;
            }
        };
    }
}
