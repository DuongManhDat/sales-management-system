package com.shop.controller;

import com.shop.model.StockAdjustment;
import com.shop.service.AdjustmentService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableRow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class StockAdjustmentListController {
    private static final Logger log = LoggerFactory.getLogger(StockAdjustmentListController.class);

    @FXML private Label lblTotal;
    @FXML private TableView<StockAdjustment> tableAdjustments;
    @FXML private TableColumn<StockAdjustment, String> colCode;
    @FXML private TableColumn<StockAdjustment, String> colDate;
    @FXML private TableColumn<StockAdjustment, String> colNote;

    private final AdjustmentService adjustmentService = new AdjustmentService();
    private final ObservableList<StockAdjustment> adjustmentList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTable();
        loadData();
    }

    private void setupTable() {
        colCode.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCode()));
        colDate.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCreatedAt()));
        colNote.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNote()));

        tableAdjustments.setItems(adjustmentList);

        tableAdjustments.setRowFactory(tv -> {
            TableRow<StockAdjustment> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    StockAdjustment rowData = row.getItem();
                    showDetailDialog(rowData);
                }
            });
            return row;
        });
    }

    private void loadData() {
        try {
            List<StockAdjustment> list = adjustmentService.getAllAdjustments();
            adjustmentList.setAll(list);
            lblTotal.setText("Tổng số: " + list.size() + " phiếu");
        } catch (SQLException e) {
            log.error("Error loading stock adjustments", e);
        }
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }

    @FXML
    private void handleCreateAdjustment() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/stock-adjustment-view.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Kiểm Kho");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
            loadData();
        } catch (IOException e) {
            log.error("Failed to open stock adjustment view", e);
        }
    }

    private void showDetailDialog(StockAdjustment adjustment) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/stock-adjustment-detail-dialog.fxml"));
            Parent root = loader.load();
            
            StockAdjustmentDetailController controller = loader.getController();
            controller.setAdjustment(adjustment);
            
            Stage stage = new Stage();
            stage.setTitle("Chi tiết Phiếu Kiểm Kho: " + adjustment.getCode());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            log.error("Failed to open stock adjustment detail view", e);
        }
    }
}
