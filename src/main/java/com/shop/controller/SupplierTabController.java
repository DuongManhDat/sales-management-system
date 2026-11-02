package com.shop.controller;

import com.shop.model.Supplier;
import com.shop.control.PaginationControl;
import com.shop.model.common.PageResult;
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
    @FXML private TableColumn<Supplier, String> statusColumn;
    @FXML private TableColumn<Supplier, Void> actionColumn;
    @FXML private PaginationControl paginationControl;
    private int currentPage = 1;
    private static final int PAGE_SIZE = 15;
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
        statusColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(com.shop.model.ActiveStatus.fromBoolean(cellData.getValue().isActive()).getDisplayName())
        );
        statusColumn.setCellFactory(com.shop.util.StatusBadge.createCellFactory());

        setupActionColumn();
        supplierTable.setItems(allSuppliers);

        paginationControl.setPageSize(PAGE_SIZE);
        paginationControl.setOnPageChangeListener(page -> {
            this.currentPage = page;
            loadData();
        });

        searchField.textProperty().addListener((obs, old, text) -> {
            currentPage = 1;
            loadData();
        });

        loadData();
    }

    private void setupActionColumn() {
        actionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button();
            private final Button deleteBtn = new Button();
            private final FontIcon editIcon = new FontIcon("fth-edit-2");
            private final FontIcon deleteIcon = new FontIcon("fth-trash-2");
            private final Tooltip editTooltip = new Tooltip("Sửa nhà cung cấp");
            private final Tooltip deleteTooltip = new Tooltip("Xóa nhà cung cấp");
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

        // Double click vào hàng để sửa nhà cung cấp
        supplierTable.setRowFactory(tv -> {
            TableRow<Supplier> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    openSupplierDialog(row.getItem());
                }
            });
            return row;
        });
    }

    public void loadData() {
        String keyword = searchField != null ? searchField.getText() : null;
        Task<PageResult<Supplier>> task = supplierService.getPagedSuppliersTask(keyword, currentPage, PAGE_SIZE);
        task.setOnSucceeded(e -> {
            PageResult<Supplier> result = task.getValue();
            allSuppliers.setAll(result.getItems());
            if (totalLabel != null) {
                totalLabel.setText("Tổng: " + result.getTotalItems() + " nhà cung cấp");
            }
            if (paginationControl != null) {
                paginationControl.updateState(result.getPageIndex(), result.getPageSize(), result.getTotalItems());
            }
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
