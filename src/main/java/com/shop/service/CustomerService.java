package com.shop.service;

import com.shop.dao.CustomerDao;
import com.shop.dao.InvoiceDao;
import com.shop.model.Customer;
import javafx.concurrent.Task;

import java.util.List;

public class CustomerService {
    private final CustomerDao customerDao;
    private final InvoiceDao invoiceDao;

    public CustomerService() {
        this.customerDao = new CustomerDao();
        this.invoiceDao = new InvoiceDao();
    }

    public Task<List<Customer>> getAllActiveCustomersTask() {
        return new Task<List<Customer>>() {
            @Override
            protected List<Customer> call() throws Exception {
                return customerDao.getAllActive();
            }
        };
    }
    
    public Task<List<Customer>> getAllCustomersTask() {
        return new Task<List<Customer>>() {
            @Override
            protected List<Customer> call() throws Exception {
                return customerDao.findAll();
            }
        };
    }

    public Task<Void> saveCustomerTask(Customer customer) {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                if (customer.getId() == 0) {
                    customerDao.insert(customer);
                } else {
                    customerDao.update(customer);
                }
                return null;
            }
        };
    }

    public Task<Void> deleteCustomerTask(int customerId) {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                long debt = invoiceDao.getTotalDebtByCustomerId(customerId);
                if (debt > 0) {
                    throw new IllegalStateException("Không thể xóa khách hàng đang có công nợ (" + debt + ").");
                }
                customerDao.setActive(customerId, false);
                return null;
            }
        };
    }
}
