package com.shop.controller;

import com.shop.model.Product;
import com.shop.service.ProductService;
import com.shop.viewmodel.ProductListViewModel;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Locale;

public class ProductListController {

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
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

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
        
        colStock.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.2f", data.getValue().getStockQty())));
        
        colStatus.setCellValueFactory(data -> {
            boolean isDeleted = data.getValue().getDeletedAt() != null;
            return new SimpleStringProperty(isDeleted ? "Đã xóa" : "Hoạt động");
        });

        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("Sửa");
            private final Button btnDelete = new Button("Xóa");
            private final HBox pane = new HBox(8, btnEdit, btnDelete);

            {
                btnEdit.getStyleClass().addAll("btn-secondary", "small");
                btnDelete.getStyleClass().addAll("btn-secondary", "small");

                btnEdit.setOnAction(event -> {
                    Product product = getTableView().getItems().get(getIndex());
                    showProductForm(product);
                });

                btnDelete.setOnAction(event -> {
                    Product product = getTableView().getItems().get(getIndex());
                    handleDelete(product);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Product p = getTableView().getItems().get(getIndex());
                    if (p.getDeletedAt() != null) {
                        btnDelete.setText("Khôi phục");
                        btnDelete.setOnAction(e -> handleRestore(p));
                    } else {
                        btnDelete.setText("Xóa");
                        btnDelete.setOnAction(e -> handleDelete(p));
                    }
                    setGraphic(pane);
                }
            }
        });
    }

    private void loadData() {
        try {
            viewModel.setProducts(productService.getAllProducts());
        } catch (SQLException e) {
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
            showAlert("Lỗi", "Không thể mở form: " + e.getMessage());
        }
    }

    private void handleDelete(Product product) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận");
        alert.setHeaderText(null);
        alert.setContentText("Bạn có chắc chắn muốn xóa (ẩn) hàng hóa này không?");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    productService.softDelete(product.getId());
                    loadData();
                } catch (Exception e) {
                    showAlert("Lỗi", "Không thể xóa: " + e.getMessage());
                }
            }
        });
    }

    private void handleRestore(Product product) {
        try {
            productService.restore(product.getId());
            loadData();
        } catch (Exception e) {
            showAlert("Lỗi", "Không thể khôi phục: " + e.getMessage());
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
            showAlert("Lỗi", "Không thể mở form: " + e.getMessage());
        }
    }

    @FXML
    private void handleStockAdjustment() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/stock-adjustment-view.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Kiểm kho (Điều chỉnh tồn kho)");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadData();
        } catch (IOException e) {
            showAlert("Lỗi", "Không thể mở form kiểm kho: " + e.getMessage());
        }
    }

    @FXML
    private void handleExport() {
        // Simple placeholder for export
        showAlert("Thông báo", "Chức năng Export Excel chưa được gọi UI.");
    }

    @FXML
    private void handleImport() {
        // Simple placeholder for import
        showAlert("Thông báo", "Chức năng Import Excel chưa được gọi UI.");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
