package com.shop.controller;

import com.shop.model.Product;
import com.shop.service.ImportExportService;
import com.shop.service.ProductService;
import com.shop.viewmodel.ProductListViewModel;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ProductListController {

    private static final Logger log = LoggerFactory.getLogger(ProductListController.class);

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private ComboBox<String> statusFilterCombo;

    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, String> colCode;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, String> colUnit;
    @FXML private TableColumn<Product, String> colCategory;
    @FXML private TableColumn<Product, String> colPrice;
    @FXML private TableColumn<Product, String> colStock;
    @FXML private TableColumn<Product, String> colStatus;
    @FXML private TableColumn<Product, Void> colAction;

    private ProductListViewModel viewModel;
    private ProductService productService;
    private final ImportExportService importExportService = new ImportExportService();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    @FXML
    public void initialize() {
        productService = new ProductService();
        viewModel = new ProductListViewModel();

        setupBindings();
        setupColumns();
        loadData();
    }

    private void setupBindings() {
        viewModel.searchKeywordProperty().bind(searchField.textProperty());
        viewModel.filterCategoryProperty().bind(categoryCombo.valueProperty());
        viewModel.filterStatusProperty().bind(statusFilterCombo.valueProperty());
        
        productTable.setItems(viewModel.getFilteredProducts());
    }

    private void setupColumns() {
        colCode.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCode()));
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colUnit.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUnitName()));
        colCategory.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategoryName()));
        
        colPrice.setCellValueFactory(data -> new SimpleStringProperty(currencyFormat.format(data.getValue().getSalePrice())));
        
        colStock.setCellValueFactory(data -> {
            double qty = data.getValue().getStockQty();
            if (qty == Math.floor(qty)) {
                return new SimpleStringProperty(String.format("%,d", (long) qty));
            } else {
                return new SimpleStringProperty(String.format("%,.2f", qty));
            }
        });
        
        colStatus.setCellValueFactory(data -> {
            boolean isDeleted = data.getValue().getDeletedAt() != null;
            return new SimpleStringProperty(isDeleted ? "Tạm ngưng" : "Hoạt động");
        });
        colStatus.setCellFactory(com.shop.util.StatusBadge.createCellFactory());

        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button();
            private final Button btnToggleStatus = new Button();
            private final FontIcon editIcon = new FontIcon("fth-edit-2");
            private final FontIcon pauseIcon = new FontIcon("fth-pause-circle");
            private final FontIcon playIcon = new FontIcon("fth-play-circle");
            private final Tooltip editTooltip = new Tooltip("Sửa hàng hóa");
            private final Tooltip pauseTooltip = new Tooltip("Tạm ngưng kinh doanh");
            private final Tooltip playTooltip = new Tooltip("Kích hoạt lại kinh doanh");
            private final HBox pane = new HBox(6, btnEdit, btnToggleStatus);

            {
                pane.setAlignment(javafx.geometry.Pos.CENTER);
                btnEdit.setGraphic(editIcon);
                btnEdit.setTooltip(editTooltip);
                btnEdit.getStyleClass().addAll("btn-icon-action", "btn-icon-edit");

                btnToggleStatus.getStyleClass().add("btn-icon-action");

                btnEdit.setOnAction(event -> {
                    Product product = getTableView().getItems().get(getIndex());
                    showProductForm(product);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Product p = getTableView().getItems().get(getIndex());
                    btnToggleStatus.getStyleClass().removeAll("btn-icon-pause", "btn-icon-play", "btn-icon-delete", "btn-icon-restore");
                    if (p.getDeletedAt() != null) {
                        btnToggleStatus.setGraphic(playIcon);
                        btnToggleStatus.setTooltip(playTooltip);
                        btnToggleStatus.getStyleClass().add("btn-icon-play");
                        btnToggleStatus.setOnAction(e -> handleRestore(p));
                    } else {
                        btnToggleStatus.setGraphic(pauseIcon);
                        btnToggleStatus.setTooltip(pauseTooltip);
                        btnToggleStatus.getStyleClass().add("btn-icon-pause");
                        btnToggleStatus.setOnAction(e -> handleDelete(p));
                    }
                    setGraphic(pane);
                }
            }
        });

        // Double click vào hàng để sửa/xem hàng hóa
        productTable.setRowFactory(tv -> {
            TableRow<Product> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    showProductForm(row.getItem());
                }
            });
            return row;
        });
    }

    private void loadData() {
        try {
            viewModel.setProducts(productService.getAllProducts());
        } catch (SQLException e) {
            log.error("Lỗi khi tải danh sách hàng hóa: {}", e.getMessage(), e);
            showAlert("Lỗi", "Không thể tải danh sách hàng hóa: " + e.getMessage());
        }
    }

    @FXML
    private void handleAdd() {
        showProductForm(null);
    }

    private void showProductForm(Product product) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/product-form-dialog.fxml"));
            Parent root = loader.load();

            ProductFormController controller = loader.getController();
            controller.setProduct(product);

            Stage stage = new Stage();
            stage.setTitle(product == null ? "Thêm hàng hóa" : "Sửa hàng hóa");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadData();
        } catch (IOException e) {
            log.error("Lỗi khi mở giao diện form hàng hóa: {}", e.getMessage(), e);
            showAlert("Lỗi", "Không thể mở form: " + e.getMessage());
        }
    }

    private void handleDelete(Product product) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận tạm ngưng");
        alert.setHeaderText(null);
        alert.setContentText("Bạn có chắc chắn muốn tạm ngưng kinh doanh hàng hóa \"" + product.getName() + "\" không?");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    productService.softDelete(product.getId());
                    loadData();
                } catch (Exception e) {
                    log.error("Lỗi khi tạm ngưng hàng hóa ID={}: {}", product.getId(), e.getMessage(), e);
                    showAlert("Lỗi", "Không thể tạm ngưng: " + e.getMessage());
                }
            }
        });
    }

    private void handleRestore(Product product) {
        try {
            productService.restore(product.getId());
            loadData();
        } catch (Exception e) {
            log.error("Lỗi khi kích hoạt lại hàng hóa ID={}: {}", product.getId(), e.getMessage(), e);
            showAlert("Lỗi", "Không thể kích hoạt lại: " + e.getMessage());
        }
    }

    @FXML
    private void handlePriceHistory() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Thông báo", "Vui lòng chọn một hàng hóa để xem lịch sử giá.");
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/product-price-history-dialog.fxml"));
            Parent root = loader.load();

            PriceHistoryController controller = loader.getController();
            controller.setProduct(selected);

            Stage stage = new Stage();
            stage.setTitle("Lịch sử giá: " + selected.getName());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            log.error("Lỗi khi mở form lịch sử giá cho hàng hóa ID={}: {}", selected.getId(), e.getMessage(), e);
            showAlert("Lỗi", "Không thể mở form: " + e.getMessage());
        }
    }

    @FXML
    private void handleStockAdjustment() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/stock-adjustment-view.fxml"));
            Parent root = loader.load();

            StockAdjustmentController controller = loader.getController();
            Product selected = productTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                controller.addProductDirectly(selected);
            }

            Stage stage = new Stage();
            stage.setTitle("Kiểm kho (Điều chỉnh tồn kho)");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadData();
        } catch (IOException e) {
            log.error("Lỗi khi mở form kiểm kho: {}", e.getMessage(), e);
            showAlert("Lỗi", "Không thể mở form kiểm kho: " + e.getMessage());
        }
    }

    @FXML
    private void handleDownloadTemplate() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Lưu file Excel mẫu nhập hàng hóa");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx"));
        fileChooser.setInitialFileName("mau-nhap-hang-hoa.xlsx");

        Stage stage = (Stage) productTable.getScene().getWindow();
        File saveFile = fileChooser.showSaveDialog(stage);
        if (saveFile != null) {
            try {
                importExportService.generateImportTemplate(saveFile);
                showAlert("Thành công", "Đã tải file mẫu Excel thành công!");
            } catch (Exception e) {
                log.error("Lỗi khi tạo file mẫu Excel: {}", e.getMessage(), e);
                showAlert("Lỗi", "Không thể tạo file mẫu: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleExport() {
        List<Product> items = productTable.getItems();
        if (items == null || items.isEmpty()) {
            showAlert("Thông báo", "Không có dữ liệu hàng hóa để xuất Excel.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Xuất danh sách hàng hóa ra Excel");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx"));
        fileChooser.setInitialFileName("danh-sach-hang-hoa.xlsx");

        Stage stage = (Stage) productTable.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            Task<Void> exportTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    importExportService.exportProducts(file, items);
                    return null;
                }
            };

            exportTask.setOnSucceeded(e -> Platform.runLater(() ->
                    showAlert("Thành công", "Xuất thành công!")
            ));

            exportTask.setOnFailed(e -> Platform.runLater(() -> {
                Throwable ex = exportTask.getException();
                log.error("Lỗi khi xuất danh sách hàng hóa ra Excel: {}", ex.getMessage(), ex);
                showAlert("Lỗi", "Không thể xuất file Excel: " + ex.getMessage());
            }));

            new Thread(exportTask).start();
        }
    }

    @FXML
    private void handleImport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn file Excel danh sách hàng hóa cần nhập");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx"));

        Stage stage = (Stage) productTable.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try {
                ImportExportService.ImportResult result = importExportService.validateImport(file);

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/product-import-dialog.fxml"));
                Parent root = loader.load();

                ProductImportDialogController dialogController = loader.getController();
                dialogController.setData(file, result, this::loadData);

                Stage dialogStage = new Stage();
                dialogStage.setTitle("Xem trước nhập file Excel: " + file.getName());
                dialogStage.initModality(Modality.APPLICATION_MODAL);
                dialogStage.initOwner(stage);
                dialogStage.setScene(new Scene(root));
                dialogStage.showAndWait();

            } catch (Exception e) {
                log.error("Lỗi khi đọc file hoặc mở hộp thoại nhập Excel: {}", e.getMessage(), e);
                showAlert("Lỗi", "Không thể đọc file Excel: " + e.getMessage());
            }
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
