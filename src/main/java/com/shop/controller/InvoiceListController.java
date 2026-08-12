package com.shop.controller;

import com.shop.dao.InvoiceDao;
import com.shop.model.Invoice;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

public class InvoiceListController {
    private static final Logger log = LoggerFactory.getLogger(InvoiceListController.class);

    @FXML private ComboBox<String> statusFilter;
    @FXML private TableView<Invoice> invoiceTable;
    @FXML private TableColumn<Invoice, String> colCode;
    @FXML private TableColumn<Invoice, String> colDate;
    @FXML private TableColumn<Invoice, String> colCustomer;
    @FXML private TableColumn<Invoice, Number> colTotal;
    @FXML private TableColumn<Invoice, Number> colPaid;
    @FXML private TableColumn<Invoice, Number> colDebt;
    @FXML private TableColumn<Invoice, String> colStatus;

    private InvoiceDao invoiceDao = new InvoiceDao();

    @FXML
    public void initialize() {
        statusFilter.setItems(FXCollections.observableArrayList("ALL", "PAID", "DEBT"));
        statusFilter.getSelectionModel().selectFirst();
        
        colCode.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCode()));
        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getInvoiceDate()));
        colCustomer.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getCustomerId())));
        colTotal.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().getTotal()));
        colPaid.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().getPaid()));
        colDebt.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().getDebt()));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));

        statusFilter.valueProperty().addListener((obs, old, val) -> loadInvoices());
        
        loadInvoices();
    }

    @FXML
    private void loadInvoices() {
        try {
            String filter = statusFilter.getValue();
            List<Invoice> invoices;
            if ("ALL".equals(filter)) {
                invoices = invoiceDao.findAll();
            } else {
                invoices = invoiceDao.findByStatus(filter);
            }
            invoiceTable.setItems(FXCollections.observableArrayList(invoices));
        } catch (SQLException e) {
            log.error("Failed to load invoices", e);
        }
    }
}
