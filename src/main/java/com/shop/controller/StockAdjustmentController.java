package com.shop.controller;

import com.shop.model.Product;
import com.shop.model.StockAdjustment;
import com.shop.model.StockAdjustmentItem;
import com.shop.service.AdjustmentService;
import com.shop.service.ProductService;
import com.shop.viewmodel.StockAdjustmentViewModel;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.util.converter.DoubleStringConverter;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StockAdjustmentController {

    @FXML private ComboBox<Product> comboProductSearch;
    @FXML private TableView<StockAdjustmentItem> adjustmentTable;
    @FXML private TableColumn<StockAdjustmentItem, String> colCode;
    @FXML private TableColumn<StockAdjustmentItem, String> colName;
    @FXML private TableColumn<StockAdjustmentItem, String> colCurrentQty;
    @FXML private TableColumn<StockAdjustmentItem, Double> colActualQty;
    @FXML private TableColumn<StockAdjustmentItem, String> colVariance;
    @FXML private TableColumn<StockAdjustmentItem, String> colReason;
    @FXML private TableColumn<StockAdjustmentItem, Void> colAction;
    
    @FXML private TextArea txtNote;

    private StockAdjustmentViewModel viewModel = new StockAdjustmentViewModel();
    private ProductService productService = new ProductService();
    private AdjustmentService adjustmentService = new AdjustmentService();

    @FXML
    public void initialize() {
        setupProductSearch();
        setupColumns();
        
        adjustmentTable.setItems(viewModel.getItems());
        txtNote.textProperty().bindBidirectional(viewModel.noteProperty());
    }

    private void setupProductSearch() {
        try {
            List<Product> products = productService.getAllProducts();
            comboProductSearch.setItems(FXCollections.observableArrayList(products));
            comboProductSearch.setConverter(new StringConverter<>() {
                @Override public String toString(Product p) { return p != null ? p.getCode() + " - " + p.getName() : ""; }
                @Override public Product fromString(String s) { return null; }
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupColumns() {
        colCode.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProductCode()));
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProductName()));
        colCurrentQty.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.2f", data.getValue().getCurrentQty())));
        
        // Editable Actual Qty
        colActualQty.setCellValueFactory(data -> javafx.beans.binding.Bindings.createObjectBinding(() -> data.getValue().getActualQty()));
        colActualQty.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colActualQty.setOnEditCommit(event -> {
            StockAdjustmentItem item = event.getRowValue();
            Double newActual = event.getNewValue();
            if (newActual != null) {
                item.setActualQty(newActual);
                item.setVariance(newActual - item.getCurrentQty());
                adjustmentTable.refresh();
            }
        });

        colVariance.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.2f", data.getValue().getVariance())));
        
        // Editable Reason
        colReason.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReason()));
        colReason.setCellFactory(TextFieldTableCell.forTableColumn());
        colReason.setOnEditCommit(event -> {
            event.getRowValue().setReason(event.getNewValue());
        });

        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnRemove = new Button("Xóa");
            {
                btnRemove.getStyleClass().addAll("btn-secondary", "small");
                btnRemove.setOnAction(event -> {
                    StockAdjustmentItem item = getTableView().getItems().get(getIndex());
                    viewModel.removeItem(item);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(btnRemove);
            }
        });
    }

    @FXML
    private void handleAddProduct() {
        Product p = comboProductSearch.getValue();
        if (p == null) return;
        
        StockAdjustmentItem item = new StockAdjustmentItem();
        item.setProductId(p.getId());
        item.setProductCode(p.getCode());
        item.setProductName(p.getName());
        item.setCurrentQty(p.getStockQty());
        item.setActualQty(p.getStockQty());
        item.setVariance(0.0);
        item.setReason("");
        item.setCostPrice(p.getSalePrice()); // Simple assumption for cost price if not available, or keep null
        
        viewModel.addItem(item);
        comboProductSearch.setValue(null);
    }

    @FXML
    private void handleSave() {
        if (viewModel.getItems().isEmpty()) {
            showAlert("Lỗi", "Phiếu kiểm kho trống.");
            return;
        }

        try {
            StockAdjustment adj = new StockAdjustment();
            adj.setNote(viewModel.noteProperty().get());
            
            adjustmentService.createAdjustment(adj, new ArrayList<>(viewModel.getItems()));
            
            showAlert("Thành công", "Lưu phiếu kiểm kho thành công.");
            handleClose();
        } catch (SQLException e) {
            showAlert("Lỗi", "Không thể lưu phiếu kiểm kho: " + e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) adjustmentTable.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
