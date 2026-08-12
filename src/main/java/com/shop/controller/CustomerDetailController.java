package com.shop.controller;

import com.shop.dao.InvoiceDao;
import com.shop.model.Customer;
import com.shop.model.Invoice;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;

import java.sql.SQLException;
import java.util.List;

public class CustomerDetailController {

    @FXML private Label lblCustomerName;
    @FXML private Label lblCode;
    @FXML private Label lblPhone;
    @FXML private Label lblEmail;
    @FXML private Label lblDateOfBirth;
    @FXML private Label lblGender;
    @FXML private Label lblAddress;
    @FXML private Label lblStatus;
    @FXML private Label lblNote;
    
    @FXML private Label lblTotalSales;
    @FXML private Label lblTotalDebt;

    @FXML private TableView<Invoice> invoiceTable;
    @FXML private TableColumn<Invoice, String> colInvoiceCode;
    @FXML private TableColumn<Invoice, String> colInvoiceDate;
    @FXML private TableColumn<Invoice, Long> colTotal;
    @FXML private TableColumn<Invoice, Long> colPaid;
    @FXML private TableColumn<Invoice, Long> colDebt;
    @FXML private TableColumn<Invoice, String> colInvoiceStatus;

    private Customer currentCustomer;
    private InvoiceDao invoiceDao;
    private ObservableList<Invoice> invoiceList;
    private Runnable onBackAction;
    private Runnable onEditAction;

    @FXML
    public void initialize() {
        invoiceDao = new InvoiceDao();
        invoiceList = FXCollections.observableArrayList();
        
        setupTable();
    }
    
    public void initData(Customer customer, Runnable onBack, Runnable onEdit) {
        this.currentCustomer = customer;
        this.onBackAction = onBack;
        this.onEditAction = onEdit;
        
        populateCustomerInfo();
        loadInvoices();
    }

    private void setupTable() {
        colInvoiceCode.setCellValueFactory(new PropertyValueFactory<>("code"));
        colInvoiceDate.setCellValueFactory(new PropertyValueFactory<>("invoiceDate"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colPaid.setCellValueFactory(new PropertyValueFactory<>("paid"));
        colDebt.setCellValueFactory(new PropertyValueFactory<>("debt"));
        colInvoiceStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        invoiceTable.setItems(invoiceList);
    }

    private void populateCustomerInfo() {
        if (currentCustomer == null) return;
        
        lblCustomerName.setText(currentCustomer.getName());
        lblCode.setText(currentCustomer.getCode());
        lblPhone.setText(currentCustomer.getPhone());
        lblEmail.setText(currentCustomer.getEmail() != null ? currentCustomer.getEmail() : "-");
        if (currentCustomer.getDateOfBirth() != null) {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy");
            lblDateOfBirth.setText(currentCustomer.getDateOfBirth().format(formatter));
        } else {
            lblDateOfBirth.setText("-");
        }
        
        String genderStr = "-";
        if (currentCustomer.getGender() != null) {
            switch (currentCustomer.getGender()) {
                case MALE: genderStr = "Nam"; break;
                case FEMALE: genderStr = "Nữ"; break;
            }
        }
        lblGender.setText(genderStr);
        lblAddress.setText(currentCustomer.getAddress() != null ? currentCustomer.getAddress() : "-");
        lblNote.setText(currentCustomer.getNote() != null ? currentCustomer.getNote() : "-");
        lblStatus.setText(currentCustomer.isActive() ? "Hoạt động" : "Ngừng hoạt động");
    }

    private void loadInvoices() {
        Task<List<Invoice>> loadTask = new Task<List<Invoice>>() {
            @Override
            protected List<Invoice> call() throws Exception {
                return invoiceDao.findByCustomerId(currentCustomer.getId());
            }
        };
        
        loadTask.setOnSucceeded(e -> {
            List<Invoice> invoices = loadTask.getValue();
            invoiceList.setAll(invoices);
            
            long totalSales = invoices.stream().mapToLong(Invoice::getTotal).sum();
            long totalDebt = invoices.stream().mapToLong(Invoice::getDebt).sum();
            
            lblTotalSales.setText(String.format("%,d đ", totalSales));
            lblTotalDebt.setText(String.format("%,d đ", totalDebt));
        });
        
        loadTask.setOnFailed(e -> {
            e.getSource().getException().printStackTrace();
            // Handle error silently or show alert
        });
        
        new Thread(loadTask).start();
    }

    @FXML
    private void handleBack() {
        if (onBackAction != null) onBackAction.run();
    }

    @FXML
    private void handleEdit() {
        if (onEditAction != null) onEditAction.run();
    }
}
