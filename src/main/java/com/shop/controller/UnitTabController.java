package com.shop.controller;

import com.shop.model.Unit;
import com.shop.service.UnitService;
import com.shop.util.DialogHelper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Optional;

public class UnitTabController {

    @FXML private TextField searchField;
    @FXML private TableView<Unit> unitTable;
    @FXML private TableColumn<Unit, String> nameColumn;
    @FXML private TableColumn<Unit, String> statusColumn;
    @FXML private TableColumn<Unit, Void> actionColumn;
    @FXML private Label totalLabel;

    private UnitService unitService = new UnitService();
    private ObservableList<Unit> allUnits = FXCollections.observableArrayList();
    private FilteredList<Unit> filteredUnits;

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        statusColumn.setCellValueFactory(cellData -> {
            int status = cellData.getValue().getStatus();
            return new SimpleStringProperty(status == 1 ? "ACTIVE" : "INACTIVE");
        });

        setupActionColumn();

        filteredUnits = new FilteredList<>(allUnits, u -> true);
        searchField.textProperty().addListener((obs, old, text) -> {
            filteredUnits.setPredicate(u -> 
                text == null || text.isBlank() || u.getName().toLowerCase().contains(text.toLowerCase())
            );
        });

        unitTable.setItems(filteredUnits);
        
        loadData();
    }

    private void setupActionColumn() {
        actionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button();
            private final Button deleteBtn = new Button();
            private final FontIcon editIcon = new FontIcon("fth-edit-2");
            private final FontIcon deleteIcon = new FontIcon("fth-trash-2");
            private final Tooltip editTooltip = new Tooltip("Sửa đơn vị");
            private final Tooltip deleteTooltip = new Tooltip("Xóa đơn vị");
            private final HBox pane = new HBox(6, editBtn, deleteBtn);

            {
                pane.setAlignment(javafx.geometry.Pos.CENTER);
                editBtn.setGraphic(editIcon);
                editBtn.setTooltip(editTooltip);
                editBtn.getStyleClass().addAll("btn-icon-action", "btn-icon-edit");

                deleteBtn.setGraphic(deleteIcon);
                deleteBtn.setTooltip(deleteTooltip);
                deleteBtn.getStyleClass().addAll("btn-icon-action", "btn-icon-delete");

                editBtn.setOnAction(event -> {
                    Unit unit = getTableView().getItems().get(getIndex());
                    handleEditUnit(unit);
                });
                deleteBtn.setOnAction(event -> {
                    Unit unit = getTableView().getItems().get(getIndex());
                    handleDeleteUnit(unit);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        // Double click vào hàng để sửa đơn vị
        unitTable.setRowFactory(tv -> {
            TableRow<Unit> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    handleEditUnit(row.getItem());
                }
            });
            return row;
        });
    }

    private void loadData() {
        allUnits.setAll(unitService.findAllActive());
        totalLabel.setText("Tổng: " + allUnits.size() + " đơn vị");
    }

    @FXML
    public void handleAddUnit() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Thêm đơn vị tính");
        dialog.setHeaderText("Tên đơn vị: (vd: cái, hộp, kg, lít, thùng...)");
        dialog.setContentText("Tên đơn vị:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            try {
                unitService.add(name);
                loadData();
            } catch (Exception e) {
                DialogHelper.showError("Lỗi", e.getMessage());
            }
        });
    }

    private void handleEditUnit(Unit unit) {
        TextInputDialog dialog = new TextInputDialog(unit.getName());
        dialog.setTitle("Sửa đơn vị tính");
        dialog.setHeaderText("Nhập tên mới:");
        dialog.setContentText("Tên đơn vị:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            try {
                unitService.update(unit.getId(), name);
                loadData();
            } catch (Exception e) {
                DialogHelper.showError("Lỗi", e.getMessage());
            }
        });
    }

    private void handleDeleteUnit(Unit unit) {
        DialogHelper.showConfirm("Xác nhận xóa", "Xóa đơn vị '" + unit.getName() + "'? Thao tác này không thể hoàn tác.", () -> {
            try {
                unitService.softDelete(unit.getId());
                loadData();
            } catch (Exception e) {
                DialogHelper.showError("Lỗi", e.getMessage());
            }
        });
    }
}
