package com.shop.controller;

import com.shop.dao.InvoiceDao;
import com.shop.model.Invoice;
import com.shop.util.DBConnection;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class OrderListController {
    private static final Logger log = LoggerFactory.getLogger(OrderListController.class);

    @FXML private ComboBox<String> statusFilter;
    @FXML private TableView<Invoice> orderTable;
    @FXML private TableColumn<Invoice, String> colCode;
    @FXML private TableColumn<Invoice, String> colDate;
    @FXML private TableColumn<Invoice, String> colCustomer;
    @FXML private TableColumn<Invoice, Number> colTotal;
    @FXML private TableColumn<Invoice, Number> colPaid;
    @FXML private TableColumn<Invoice, String> colStatus;

    private InvoiceDao invoiceDao = new InvoiceDao();

    @FXML
    public void initialize() {
        statusFilter.setItems(FXCollections.observableArrayList("PENDING", "CANCELLED"));
        statusFilter.getSelectionModel().selectFirst();
        
        colCode.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCode()));
        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getInvoiceDate()));
        colCustomer.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getCustomerId())));
        colTotal.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().getTotal()));
        colPaid.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().getPaid()));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));

        statusFilter.valueProperty().addListener((obs, old, val) -> loadOrders());
        
        loadOrders();
    }

    @FXML
    private void loadOrders() {
        try {
            String filter = statusFilter.getValue();
            List<Invoice> orders = invoiceDao.findByStatus(filter);
            orderTable.setItems(FXCollections.observableArrayList(orders));
        } catch (SQLException e) {
            log.error("Failed to load orders", e);
        }
    }

    @FXML
    private void completeOrder() {
        Invoice selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected == null || !"PENDING".equals(selected.getStatus())) {
            showAlert("Lỗi", "Vui lòng chọn 1 đơn PENDING");
            return;
        }
        
        try (Connection conn = DBConnection.getConnection()) {
            // Update status to COMPLETED
            invoiceDao.updatePaymentStatus(conn, selected.getId(), selected.getPaid(), selected.getDebt(), "COMPLETED");
            // NOTE: In a full system, we should also decrease stock here! For MVP, we'll just update status.
            showAlert("Thành công", "Đã hoàn thành đơn hàng " + selected.getCode());
            loadOrders();
        } catch (Exception e) {
            log.error("Lỗi khi hoàn thành đơn", e);
        }
    }

    @FXML
    private void cancelOrder() {
        Invoice selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected == null || !"PENDING".equals(selected.getStatus())) {
            showAlert("Lỗi", "Vui lòng chọn 1 đơn PENDING");
            return;
        }
        
        try (Connection conn = DBConnection.getConnection()) {
            invoiceDao.updatePaymentStatus(conn, selected.getId(), selected.getPaid(), selected.getDebt(), "CANCELLED");
            showAlert("Thành công", "Đã hủy đơn hàng " + selected.getCode());
            loadOrders();
        } catch (Exception e) {
            log.error("Lỗi khi hủy đơn", e);
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
