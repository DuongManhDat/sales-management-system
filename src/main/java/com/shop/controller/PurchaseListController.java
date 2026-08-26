package com.shop.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;

import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.util.Callback;
import com.shop.service.PurchaseService;
import com.shop.model.Purchase;
import java.text.NumberFormat;
import java.util.Locale;

public class PurchaseListController {
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TableView<Purchase> purchaseTable;
    @FXML private TableColumn<Purchase, String> colCode;
    @FXML private TableColumn<Purchase, String> colDate;
    @FXML private TableColumn<Purchase, String> colSupplier;
    @FXML private TableColumn<Purchase, Long> colTotal;
    @FXML private TableColumn<Purchase, Long> colDebt;
    @FXML private TableColumn<Purchase, Void> colAction;

    private final PurchaseService purchaseService = new PurchaseService();
    private ObservableList<Purchase> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        statusFilter.getItems().addAll("Tất cả", "Đã thanh toán", "Còn nợ");
        statusFilter.getSelectionModel().selectFirst();
        
        setupTable();
        
        searchField.textProperty().addListener((observable, oldValue, newValue) -> loadData());
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> loadData());
        
        loadData();
    }
    
    private void setupTable() {
        colCode.setCellValueFactory(new PropertyValueFactory<>("code"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("purchaseDate"));
        colSupplier.setCellValueFactory(new PropertyValueFactory<>("supplierName"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalCost"));
        colDebt.setCellValueFactory(new PropertyValueFactory<>("debt"));
        
        NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
        Callback<TableColumn<Purchase, Long>, TableCell<Purchase, Long>> currencyCellFactory = tc -> new TableCell<Purchase, Long>() {
            @Override
            protected void updateItem(Long price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(currencyFormat.format(price));
                }
            }
        };
        colTotal.setCellFactory(currencyCellFactory);
        colDebt.setCellFactory(currencyCellFactory);
        
        // Setup actions column
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button viewBtn = new Button("Xem");
            {
                viewBtn.getStyleClass().add("button-secondary");
                viewBtn.setOnAction(event -> {
                    Purchase purchase = getTableView().getItems().get(getIndex());
                    // TODO: View detail
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(viewBtn);
                }
            }
        });
    }
    
    public void loadData() {
        String search = searchField.getText();
        String status = statusFilter.getValue();
        masterData.setAll(purchaseService.getAllPurchases(search, status));
        purchaseTable.setItems(masterData);
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
            // Reload data after closing the form
            stage.setOnHidden(e -> loadData());
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
