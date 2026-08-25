package com.shop.controller;

import com.shop.model.PriceHistory;
import com.shop.model.Product;
import com.shop.service.ProductService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class PriceHistoryController {

    @FXML private Label lblTitle;
    @FXML private TableView<PriceHistory> historyTable;
    @FXML private TableColumn<PriceHistory, String> colDate;
    @FXML private TableColumn<PriceHistory, String> colOldPrice;
    @FXML private TableColumn<PriceHistory, String> colNewPrice;

    private ProductService productService = new ProductService();
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    public void setProduct(Product product) {
        lblTitle.setText("Lịch sử giá bán - " + product.getName());
        setupColumns();
        loadData(product.getId());
    }

    private void setupColumns() {
        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getChangedAt()));
        colOldPrice.setCellValueFactory(data -> new SimpleStringProperty(currencyFormat.format(data.getValue().getOldPrice())));
        colNewPrice.setCellValueFactory(data -> new SimpleStringProperty(currencyFormat.format(data.getValue().getNewPrice())));
    }

    private void loadData(int productId) {
        try {
            List<PriceHistory> historyList = productService.getPriceHistory(productId);
            historyTable.setItems(FXCollections.observableArrayList(historyList));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) historyTable.getScene().getWindow();
        stage.close();
    }
}
