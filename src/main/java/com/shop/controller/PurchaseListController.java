package com.shop.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;

public class PurchaseListController {
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TableView<?> purchaseTable;
    @FXML private TableColumn<?, ?> colCode;
    @FXML private TableColumn<?, ?> colDate;
    @FXML private TableColumn<?, ?> colSupplier;
    @FXML private TableColumn<?, ?> colTotal;
    @FXML private TableColumn<?, ?> colDebt;
    @FXML private TableColumn<?, ?> colAction;

    @FXML
    public void initialize() {
        statusFilter.getItems().addAll("Tất cả", "Đã trả", "Còn nợ");
        statusFilter.getSelectionModel().selectFirst();
    }

    @FXML
    public void handleCreatePurchase() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/purchase-form.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setTitle("Tạo phiếu nhập mới");
            stage.setScene(new javafx.scene.Scene(root, 900, 600));
            stage.showAndWait();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
