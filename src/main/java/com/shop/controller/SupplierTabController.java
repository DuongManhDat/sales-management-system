package com.shop.controller;

import com.shop.model.Supplier;
import com.shop.service.SupplierService;
import com.shop.util.DialogHelper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.util.List;

public class SupplierTabController {

    @FXML private TextField searchField;
    @FXML private TableView<Supplier> supplierTable;
    @FXML private TableColumn<Supplier, String> codeColumn;
    @FXML private TableColumn<Supplier, String> nameColumn;
    @FXML private TableColumn<Supplier, String> phoneColumn;
    @FXML private TableColumn<Supplier, String> addressColumn;
    @FXML private TableColumn<Supplier, String> noteColumn;
    @FXML private TableColumn<Supplier, Void> actionColumn;
    @FXML private Label totalLabel;

    private final SupplierService supplierService = new SupplierService();
    private final ObservableList<Supplier> allSuppliers = FXCollections.observableArrayList();
    private FilteredList<Supplier> filteredSuppliers;

    @FXML
    public void initialize() {
        codeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCode()));
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        phoneColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPhone()));
        addressColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAddress()));
        noteColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNote()));

        setupActionColumn();

        filteredSuppliers = new FilteredList<>(allSuppliers, s -> true);
        searchField.textProperty().addListener((obs, old, text) -> {
            filteredSuppliers.setPredicate(s -> {
                if (text == null || text.isBlank()) return true;
                String lower = text.toLowerCase();
                return (s.getName() != null && s.getName().toLowerCase().contains(lower)) ||
                       (s.getCode() != null && s.getCode().toLowerCase().contains(lower)) ||
                       (s.getPhone() != null && s.getPhone().toLowerCase().contains(lower));
            });
        });

        supplierTable.setItems(filteredSuppliers);
        loadData();
    }

    private void setupActionColumn() {
        actionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button(" Sửa", new FontIcon("fth-edit-2"));
            private final Button deleteBtn = new Button(" Xóa", new FontIcon("fth-trash-2"));
            private final HBox pane = new HBox(5, editBtn, deleteBtn);

            {
                editBtn.getStyleClass().addAll("button", "flat", "accent");
                deleteBtn.getStyleClass().addAll("button", "flat", "danger");

                editBtn.setOnAction(event -> {
                    Supplier supplier = getTableView().getItems().get(getIndex());
                    openSupplierDialog(supplier);
                });
                deleteBtn.setOnAction(event -> {
                    Supplier supplier = getTableView().getItems().get(getIndex());
                    handleDeleteSupplier(supplier);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    public void loadData() {
        Task<List<Supplier>> task = supplierService.getAllActiveSuppliersTask();
        task.setOnSucceeded(e -> {
            allSuppliers.setAll(task.getValue());
            totalLabel.setText("Tổng: " + allSuppliers.size() + " nhà cung cấp");
        });
        task.setOnFailed(e -> {
            DialogHelper.showError("Lỗi", "Không thể tải danh sách nhà cung cấp: " + task.getException().getMessage());
        });
        new Thread(task).start();
    }

    @FXML
    public void handleAddSupplier() {
        openSupplierDialog(new Supplier());
    }

    private void openSupplierDialog(Supplier supplier) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/supplier-form-dialog.fxml"));
            Parent root = loader.load();
            
            SupplierFormDialogController controller = loader.getController();
            controller.setSupplier(supplier);
            
            Stage stage = new Stage();
            stage.setTitle(supplier.getId() == 0 ? "Thêm Nhà cung cấp" : "Sửa Nhà cung cấp");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            
            stage.showAndWait();
            
            if (controller.isSaved()) {
                loadData();
            }
        } catch (IOException e) {
            DialogHelper.showError("Lỗi", "Không thể mở form: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleDeleteSupplier(Supplier supplier) {
        DialogHelper.showConfirm("Xác nhận xóa", "Xóa nhà cung cấp '" + supplier.getName() + "'? Thao tác này không thể hoàn tác.", () -> {
            Task<Void> task = supplierService.deleteSupplierTask(supplier.getId());
            task.setOnSucceeded(e -> loadData());
            task.setOnFailed(e -> DialogHelper.showError("Lỗi", task.getException().getMessage()));
            new Thread(task).start();
        });
    }
}
