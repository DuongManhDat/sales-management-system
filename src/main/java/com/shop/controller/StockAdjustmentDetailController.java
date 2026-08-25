package com.shop.controller;

import com.shop.model.StockAdjustment;
import com.shop.model.StockAdjustmentItem;
import com.shop.service.AdjustmentService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

public class StockAdjustmentDetailController {
    private static final Logger log = LoggerFactory.getLogger(StockAdjustmentDetailController.class);

    @FXML private Label lblCode;
    @FXML private Label lblDate;
    @FXML private Label lblNote;

    @FXML private TableView<StockAdjustmentItem> tableItems;
    @FXML private TableColumn<StockAdjustmentItem, String> colProductCode;
    @FXML private TableColumn<StockAdjustmentItem, String> colProductName;
    @FXML private TableColumn<StockAdjustmentItem, String> colCurrentQty;
    @FXML private TableColumn<StockAdjustmentItem, String> colActualQty;
    @FXML private TableColumn<StockAdjustmentItem, String> colVariance;
    @FXML private TableColumn<StockAdjustmentItem, String> colReason;

    private final AdjustmentService adjustmentService = new AdjustmentService();
    private final ObservableList<StockAdjustmentItem> itemList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTable();
    }

    private void setupTable() {
        colProductCode.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getProductCode()));
        colProductName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getProductName()));
        colCurrentQty.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("%.2f", cellData.getValue().getCurrentQty())));
        colActualQty.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("%.2f", cellData.getValue().getActualQty())));
        colVariance.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("%.2f", cellData.getValue().getVariance())));
        colReason.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getReason()));

        tableItems.setItems(itemList);
    }

    public void setAdjustment(StockAdjustment adjustment) {
        if (adjustment == null) return;
        lblCode.setText(adjustment.getCode());
        lblDate.setText(adjustment.getCreatedAt());
        lblNote.setText(adjustment.getNote());
        loadItems(adjustment.getId());
    }

    private void loadItems(int adjustmentId) {
        try {
            List<StockAdjustmentItem> items = adjustmentService.getAdjustmentItems(adjustmentId);
            itemList.setAll(items);
        } catch (SQLException e) {
            log.error("Failed to load adjustment items", e);
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) tableItems.getScene().getWindow();
        stage.close();
    }
}
