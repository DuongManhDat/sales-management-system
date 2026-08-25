package com.shop.controller;

import com.shop.dao.ReturnInvoiceDao;
import com.shop.model.ReturnInvoice;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

public class ReturnListController {
    private static final Logger log = LoggerFactory.getLogger(ReturnListController.class);

    @FXML private TableView<ReturnInvoice> returnTable;
    @FXML private TableColumn<ReturnInvoice, String> colCode;
    @FXML private TableColumn<ReturnInvoice, String> colInvoiceId;
    @FXML private TableColumn<ReturnInvoice, String> colDate;
    @FXML private TableColumn<ReturnInvoice, String> colCustomer;
    @FXML private TableColumn<ReturnInvoice, Number> colTotalRefund;
    @FXML private TableColumn<ReturnInvoice, Number> colFee;
    @FXML private TableColumn<ReturnInvoice, String> colStatus;

    private ReturnInvoiceDao returnInvoiceDao = new ReturnInvoiceDao();

    @FXML
    public void initialize() {
        colCode.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCode()));
        colInvoiceId.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getInvoiceId())));
        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReturnDate()));
        colCustomer.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getCustomerId())));
        colTotalRefund.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().getTotalRefund()));
        colFee.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().getReturnFee()));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));

        loadReturns();
    }

    @FXML
    private void loadReturns() {
        try {
            List<ReturnInvoice> returns = returnInvoiceDao.findAll();
            returnTable.setItems(FXCollections.observableArrayList(returns));
        } catch (SQLException e) {
            log.error("Failed to load returns", e);
        }
    }

    @FXML
    private void openReturnForm() {
        com.shop.util.SceneManager.switchScene("/fxml/return-form.fxml", "Tạo Phiếu Trả Hàng");
    }
}
