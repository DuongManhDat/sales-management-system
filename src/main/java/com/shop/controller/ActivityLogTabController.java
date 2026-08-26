package com.shop.controller;

import com.shop.model.ActivityLog;
import com.shop.service.ActivityLogService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.SimpleStringProperty;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class ActivityLogTabController {

    @FXML private TableView<ActivityLog> logTable;
    @FXML private TableColumn<ActivityLog, String> colTime;
    @FXML private TableColumn<ActivityLog, String> colAction;
    @FXML private TableColumn<ActivityLog, String> colEntity;
    @FXML private TableColumn<ActivityLog, Integer> colEntityId;
    @FXML private TableColumn<ActivityLog, String> colDetail;

    private ActivityLogService logService;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @FXML
    public void initialize() {
        logService = new ActivityLogService();

        colTime.setCellValueFactory(cellData -> {
            if (cellData.getValue().getCreatedAt() != null) {
                return new SimpleStringProperty(cellData.getValue().getCreatedAt().format(formatter));
            }
            return new SimpleStringProperty("");
        });
        
        colAction.setCellValueFactory(new PropertyValueFactory<>("action"));
        colEntity.setCellValueFactory(new PropertyValueFactory<>("entity"));
        colEntityId.setCellValueFactory(new PropertyValueFactory<>("entityId"));
        colDetail.setCellValueFactory(new PropertyValueFactory<>("detail"));

        loadData();
    }

    @FXML
    public void handleRefresh() {
        loadData();
    }

    private void loadData() {
        List<ActivityLog> logs = logService.getAllLogs();
        ObservableList<ActivityLog> observableLogs = FXCollections.observableArrayList(logs);
        logTable.setItems(observableLogs);
    }
}
