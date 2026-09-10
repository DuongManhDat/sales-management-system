package com.shop.controller;

import com.shop.model.Product;
import com.shop.model.Purchase;
import com.shop.model.PurchaseItem;
import com.shop.model.Supplier;
import com.shop.service.ProductService;
import com.shop.service.PurchaseService;
import com.shop.service.SupplierService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.util.converter.IntegerStringConverter;
import javafx.util.converter.LongStringConverter;

import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class PurchaseFormController {

    @FXML private TextField txtCode;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<Supplier> cbSupplier;
    @FXML private TextField txtNote;

    @FXML private ComboBox<Product> comboProductSearch;
    @FXML private TableView<PurchaseItem> itemsTable;
    @FXML private TableColumn<PurchaseItem, String> colProduct;
    @FXML private TableColumn<PurchaseItem, String> colUnit;
    @FXML private TableColumn<PurchaseItem, Integer> colQty;
    @FXML private TableColumn<PurchaseItem, Long> colPrice;
    @FXML private TableColumn<PurchaseItem, Long> colAmount;
    @FXML private TableColumn<PurchaseItem, Void> colAction;

    @FXML private Label lblTotal;
    @FXML private TextField txtPaid;
    @FXML private Label lblDebt;

    private final SupplierService supplierService = new SupplierService();
    private final ProductService productService = new ProductService();
    private final PurchaseService purchaseService = new PurchaseService();

    private final ObservableList<PurchaseItem> itemsList = FXCollections.observableArrayList();
    private final ObservableList<Product> masterProductList = FXCollections.observableArrayList();
    private FilteredList<Product> filteredProductList;

    private final NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
    private long currentTotalCost = 0;

    @FXML
    public void initialize() {
        // Tự động sinh mã phiếu nhập hàng
        String autoCode = "PN" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        txtCode.setText(autoCode);

        datePicker.setValue(LocalDate.now());

        loadSuppliers();
        loadProducts();
        setupProductSearch();
        setupTable();

        txtPaid.textProperty().addListener((obs, oldV, newV) -> calculateTotals());
    }

    private void loadSuppliers() {
        try {
            List<Supplier> suppliers = supplierService.findAllActive();
            cbSupplier.setItems(FXCollections.observableArrayList(suppliers));

            cbSupplier.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(Supplier item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        String phone = item.getPhone() != null && !item.getPhone().isEmpty() ? " - " + item.getPhone() : "";
                        setText(item.getName() + phone);
                    }
                }
            });

            cbSupplier.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(Supplier item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        String phone = item.getPhone() != null && !item.getPhone().isEmpty() ? " - " + item.getPhone() : "";
                        setText(item.getName() + phone);
                    }
                }
            });
        } catch (SQLException e) {
            showAlert("Lỗi", "Không thể tải danh sách nhà cung cấp: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void loadProducts() {
        try {
            masterProductList.setAll(productService.searchActiveProducts(""));
        } catch (SQLException e) {
            showAlert("Lỗi", "Không thể tải danh sách sản phẩm: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void setupProductSearch() {
        filteredProductList = new FilteredList<>(masterProductList, p -> true);
        comboProductSearch.setItems(filteredProductList);

        comboProductSearch.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getCode() + " - " + item.getName());
                }
            }
        });

        comboProductSearch.setConverter(new StringConverter<>() {
            @Override
            public String toString(Product p) {
                return p == null ? "" : (p.getCode() + " - " + p.getName());
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

        comboProductSearch.getEditor().setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleAddProduct();
            }
        });
    }

    private void setupTable() {
        itemsTable.setItems(itemsList);
        itemsTable.setEditable(true);

        colProduct.setCellValueFactory(data -> {
            PurchaseItem item = data.getValue();
            String code = item.getProductCode() != null ? item.getProductCode() + " - " : "";
            return new SimpleStringProperty(code + item.getProductName());
        });

        colUnit.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUnitName() != null ? data.getValue().getUnitName() : ""));

        colQty.setCellValueFactory(new PropertyValueFactory<>("qty"));
        colQty.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter() {
            @Override
            public Integer fromString(String value) {
                try {
                    if (value == null || value.trim().isEmpty()) return 1;
                    int val = Integer.parseInt(value.trim().replaceAll("[^0-9]", ""));
                    return Math.max(1, val);
                } catch (Exception e) {
                    return 1;
                }
            }
        }));
        colQty.setOnEditCommit(event -> {
            PurchaseItem item = event.getRowValue();
            Integer newQty = event.getNewValue();
            item.setQty(newQty != null && newQty > 0 ? newQty : 1);
            item.setAmount(item.getQty() * item.getCostPrice());
            itemsTable.refresh();
            calculateTotals();
        });

        colPrice.setCellValueFactory(new PropertyValueFactory<>("costPrice"));
        colPrice.setCellFactory(TextFieldTableCell.forTableColumn(new LongStringConverter() {
            @Override
            public Long fromString(String value) {
                try {
                    if (value == null || value.trim().isEmpty()) return 0L;
                    long val = Long.parseLong(value.trim().replaceAll("[^0-9]", ""));
                    return Math.max(0, val);
                } catch (Exception e) {
                    return 0L;
                }
            }
        }));
        colPrice.setOnEditCommit(event -> {
            PurchaseItem item = event.getRowValue();
            Long newPrice = event.getNewValue();
            item.setCostPrice(newPrice != null && newPrice >= 0 ? newPrice : 0L);
            item.setAmount(item.getQty() * item.getCostPrice());
            itemsTable.refresh();
            calculateTotals();
        });

        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colAmount.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Long amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText(null);
                } else {
                    setText(currencyFormat.format(amount) + " đ");
                }
            }
        });

        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button("Xóa");
            {
                deleteBtn.getStyleClass().add("btn-danger");
                deleteBtn.setOnAction(event -> {
                    itemsList.remove(getIndex());
                    calculateTotals();
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });
    }

    @FXML
    public void handleAddProduct() {
        Product selected = comboProductSearch.getSelectionModel().getSelectedItem();
        if (selected == null) {
            String text = comboProductSearch.getEditor().getText();
            if (text != null && !text.trim().isEmpty()) {
                selected = comboProductSearch.getConverter().fromString(text.trim());
            }
        }
        if (selected == null && !filteredProductList.isEmpty()) {
            selected = filteredProductList.get(0);
        }

        if (selected == null) {
            showAlert("Thông báo", "Vui lòng chọn hoặc gõ tên/mã hàng hóa cần nhập.", Alert.AlertType.WARNING);
            return;
        }

        // Nếu hàng hóa đã có trong bảng thì tăng số lượng lên 1
        for (PurchaseItem existing : itemsList) {
            if (existing.getProductId() == selected.getId()) {
                existing.setQty(existing.getQty() + 1);
                existing.setAmount(existing.getQty() * existing.getCostPrice());
                itemsTable.refresh();
                calculateTotals();
                comboProductSearch.getEditor().clear();
                return;
            }
        }

        PurchaseItem item = new PurchaseItem();
        item.setProductId(selected.getId());
        item.setProductCode(selected.getCode());
        item.setProductName(selected.getName());
        item.setUnitName(selected.getUnitName());
        item.setQty(1);
        item.setCostPrice(0);
        item.setAmount(0);

        itemsList.add(item);
        calculateTotals();
        comboProductSearch.getEditor().clear();
    }

    private void calculateTotals() {
        long total = 0;
        for (PurchaseItem item : itemsList) {
            total += item.getAmount();
        }
        currentTotalCost = total;
        lblTotal.setText(currencyFormat.format(total) + " đ");

        long paid = 0;
        try {
            String rawPaid = txtPaid.getText();
            if (rawPaid != null && !rawPaid.trim().isEmpty()) {
                paid = Long.parseLong(rawPaid.trim().replaceAll("[^0-9]", ""));
            }
        } catch (NumberFormatException ignored) {}

        long debt = total - paid;
        if (debt < 0) debt = 0;
        lblDebt.setText(currencyFormat.format(debt) + " đ");
    }

    @FXML
    public void handlePayFull() {
        txtPaid.setText(String.valueOf(currentTotalCost));
        calculateTotals();
    }

    @FXML
    public void handleSave() {
        if (cbSupplier.getValue() == null) {
            showAlert("Lỗi", "Vui lòng chọn nhà cung cấp.", Alert.AlertType.ERROR);
            return;
        }
        if (itemsList.isEmpty()) {
            showAlert("Lỗi", "Vui lòng thêm ít nhất 1 sản phẩm vào phiếu nhập.", Alert.AlertType.ERROR);
            return;
        }

        for (PurchaseItem item : itemsList) {
            if (item.getQty() <= 0) {
                showAlert("Lỗi", "Số lượng sản phẩm " + item.getProductName() + " phải lớn hơn 0.", Alert.AlertType.ERROR);
                return;
            }
            if (item.getCostPrice() < 0) {
                showAlert("Lỗi", "Giá nhập sản phẩm " + item.getProductName() + " phải lớn hơn hoặc bằng 0.", Alert.AlertType.ERROR);
                return;
            }
        }

        Purchase purchase = new Purchase();
        String code = txtCode.getText() != null && !txtCode.getText().trim().isEmpty()
                ? txtCode.getText().trim()
                : "PN" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        purchase.setCode(code);
        purchase.setSupplierId(cbSupplier.getValue().getId());

        LocalDate selectedDate = datePicker.getValue() != null ? datePicker.getValue() : LocalDate.now();
        String purchaseDate = selectedDate.atTime(LocalTime.now()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        purchase.setPurchaseDate(purchaseDate);

        purchase.setNote(txtNote.getText() != null ? txtNote.getText().trim() : "");

        long paid = 0;
        try {
            String rawPaid = txtPaid.getText();
            if (rawPaid != null && !rawPaid.trim().isEmpty()) {
                paid = Long.parseLong(rawPaid.trim().replaceAll("[^0-9]", ""));
            }
        } catch (NumberFormatException ignored) {}
        purchase.setPaid(paid);

        try {
            purchaseService.createPurchase(purchase, itemsList);
            showAlert("Thành công", "Lưu phiếu nhập kho " + purchase.getCode() + " thành công!", Alert.AlertType.INFORMATION);
            handleBack();
        } catch (Exception e) {
            showAlert("Lỗi", "Lỗi khi lưu phiếu nhập: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void handleBack() {
        Stage stage = null;
        if (lblTotal != null && lblTotal.getScene() != null) {
            stage = (Stage) lblTotal.getScene().getWindow();
        }
        if (stage != null) {
            stage.close();
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
