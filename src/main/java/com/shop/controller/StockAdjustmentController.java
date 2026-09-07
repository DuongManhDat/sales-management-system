package com.shop.controller;

import com.shop.model.Product;
import com.shop.model.StockAdjustment;
import com.shop.model.StockAdjustmentItem;
import com.shop.service.AdjustmentService;
import com.shop.service.ProductService;
import com.shop.viewmodel.StockAdjustmentViewModel;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StockAdjustmentController {

    private static final Logger log = LoggerFactory.getLogger(StockAdjustmentController.class);

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

    private final StockAdjustmentViewModel viewModel = new StockAdjustmentViewModel();
    private final ProductService productService = new ProductService();
    private final AdjustmentService adjustmentService = new AdjustmentService();

    private final ObservableList<Product> masterProductList = FXCollections.observableArrayList();
    private FilteredList<Product> filteredProductList;

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
            masterProductList.setAll(products);
            filteredProductList = new FilteredList<>(masterProductList, p -> true);
            comboProductSearch.setItems(filteredProductList);

            comboProductSearch.setConverter(new StringConverter<>() {
                @Override
                public String toString(Product p) {
                    return p != null ? p.getCode() + " - " + p.getName() : "";
                }

                @Override
                public Product fromString(String s) {
                    if (s == null || s.trim().isEmpty()) {
                        return null;
                    }
                    String clean = s.trim();
                    for (Product p : masterProductList) {
                        String display = p.getCode() + " - " + p.getName();
                        if (display.equalsIgnoreCase(clean)
                                || (p.getCode() != null && p.getCode().equalsIgnoreCase(clean))
                                || (p.getName() != null && p.getName().equalsIgnoreCase(clean))) {
                            return p;
                        }
                    }
                    return null;
                }
            });

            // Lắng nghe khi gõ ký tự vào ô tìm kiếm để tự động lọc danh sách
            comboProductSearch.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
                Product selected = comboProductSearch.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    String selectedLabel = selected.getCode() + " - " + selected.getName();
                    if (selectedLabel.equalsIgnoreCase(newVal)) {
                        return;
                    }
                }

                Platform.runLater(() -> {
                    String query = comboProductSearch.getEditor().getText();
                    if (query == null || query.trim().isEmpty()) {
                        filteredProductList.setPredicate(p -> true);
                    } else {
                        String lower = query.trim().toLowerCase();
                        filteredProductList.setPredicate(p ->
                                (p.getCode() != null && p.getCode().toLowerCase().contains(lower))
                                || (p.getName() != null && p.getName().toLowerCase().contains(lower))
                        );
                    }

                    if (comboProductSearch.isFocused() && !filteredProductList.isEmpty()) {
                        comboProductSearch.show();
                    }
                });
            });

            // Bấm ENTER trong ô tìm kiếm để tự động thêm hàng hóa vào phiếu
            comboProductSearch.getEditor().setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    handleAddProduct();
                }
            });

        } catch (SQLException e) {
            log.error("Lỗi khi tải danh sách hàng hóa cho kiểm kho: {}", e.getMessage(), e);
            showAlert("Lỗi", "Không thể tải danh sách hàng hóa: " + e.getMessage());
        }
    }

    private void setupColumns() {
        colCode.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProductCode()));
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProductName()));
        colCurrentQty.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.2f", data.getValue().getCurrentQty())));
        
        // Cột Tồn thực tế - có thể chỉnh sửa trực tiếp trên bảng
        colActualQty.setCellValueFactory(data -> javafx.beans.binding.Bindings.createObjectBinding(() -> data.getValue().getActualQty()));
        colActualQty.setCellFactory(TextFieldTableCell.forTableColumn(new StringConverter<Double>() {
            @Override
            public String toString(Double val) {
                return val == null ? "0.00" : String.format("%.2f", val);
            }

            @Override
            public Double fromString(String str) {
                if (str == null || str.trim().isEmpty()) {
                    return 0.0;
                }
                try {
                    return Double.parseDouble(str.trim().replace(",", "."));
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            }
        }));
        colActualQty.setOnEditCommit(event -> {
            StockAdjustmentItem item = event.getRowValue();
            Double newActual = event.getNewValue();
            if (newActual != null && newActual >= 0) {
                item.setActualQty(newActual);
                item.setVariance(newActual - item.getCurrentQty());
                adjustmentTable.refresh();
            } else {
                showAlert("Cảnh báo", "Số lượng tồn thực tế phải là số không âm.");
                adjustmentTable.refresh();
            }
        });

        // Cột Lệch (hiển thị rõ dấu + khi thừa và - khi thiếu)
        colVariance.setCellValueFactory(data -> {
            double var = data.getValue().getVariance();
            String prefix = var > 0 ? "+" : "";
            return new SimpleStringProperty(prefix + String.format("%.2f", var));
        });
        
        // Cột Lý do - có thể chỉnh sửa trực tiếp
        colReason.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReason()));
        colReason.setCellFactory(TextFieldTableCell.forTableColumn());
        colReason.setOnEditCommit(event -> {
            event.getRowValue().setReason(event.getNewValue());
        });

        // Cột Thao tác - Nút Xóa
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
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnRemove);
                }
            }
        });
    }

    @FXML
    private void handleAddProduct() {
        Product p = comboProductSearch.getValue();

        // Nếu chưa chọn từ popup hoặc converter chưa bind kịp, tìm kiếm từ text editor
        if (p == null) {
            String text = comboProductSearch.getEditor().getText();
            if (text != null && !text.trim().isEmpty()) {
                String clean = text.trim();
                // 1. Tìm khớp chính xác mã hoặc nhãn hiển thị
                for (Product item : masterProductList) {
                    String display = item.getCode() + " - " + item.getName();
                    if (clean.equalsIgnoreCase(display) || (item.getCode() != null && item.getCode().equalsIgnoreCase(clean))) {
                        p = item;
                        break;
                    }
                }
                // 2. Tìm khớp chính xác tên
                if (p == null) {
                    for (Product item : masterProductList) {
                        if (item.getName() != null && item.getName().equalsIgnoreCase(clean)) {
                            p = item;
                            break;
                        }
                    }
                }
                // 3. Tìm sản phẩm đầu tiên có mã hoặc tên chứa từ khóa
                if (p == null) {
                    String lower = clean.toLowerCase();
                    for (Product item : masterProductList) {
                        if ((item.getCode() != null && item.getCode().toLowerCase().contains(lower))
                                || (item.getName() != null && item.getName().toLowerCase().contains(lower))) {
                            p = item;
                            break;
                        }
                    }
                }
            }
        }

        if (p == null) {
            showAlert("Thông báo", "Vui lòng chọn hoặc nhập mã/tên hàng hóa hợp lệ để thêm vào phiếu kiểm kho.");
            comboProductSearch.requestFocus();
            return;
        }

        addProductDirectly(p);

        // Xóa sạch ô tìm kiếm để người dùng tiếp tục nhập sản phẩm tiếp theo
        comboProductSearch.setValue(null);
        comboProductSearch.getEditor().clear();
        if (filteredProductList != null) {
            filteredProductList.setPredicate(item -> true);
        }
        comboProductSearch.requestFocus();
    }

    /**
     * Thêm sản phẩm trực tiếp vào danh sách kiểm kho (hỗ trợ gọi từ màn hình danh sách hàng hóa)
     */
    public void addProductDirectly(Product p) {
        if (p == null) return;

        StockAdjustmentItem item = new StockAdjustmentItem();
        item.setProductId(p.getId());
        item.setProductCode(p.getCode());
        item.setProductName(p.getName());
        item.setCurrentQty(p.getStockQty());
        item.setActualQty(p.getStockQty());
        item.setVariance(0.0);
        item.setReason("");
        item.setCostPrice(p.getSalePrice());

        viewModel.addItem(item);
        adjustmentTable.refresh();
        adjustmentTable.getSelectionModel().select(item);
        adjustmentTable.scrollTo(item);
    }

    @FXML
    private void handleSave() {
        if (viewModel.getItems().isEmpty()) {
            showAlert("Lỗi", "Phiếu kiểm kho trống. Vui lòng thêm ít nhất một hàng hóa.");
            return;
        }

        try {
            StockAdjustment adj = new StockAdjustment();
            adj.setNote(viewModel.noteProperty().get());
            
            adjustmentService.createAdjustment(adj, new ArrayList<>(viewModel.getItems()));
            
            showAlert("Thành công", "Lưu phiếu kiểm kho thành công.");
            handleClose();
        } catch (SQLException e) {
            log.error("Không thể lưu phiếu kiểm kho: {}", e.getMessage(), e);
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
