package com.shop.controller;

import com.shop.model.Product;
import com.shop.model.Purchase;
import com.shop.model.PurchaseItem;
import com.shop.model.Supplier;
import com.shop.service.ProductService;
import com.shop.service.PurchaseService;
import com.shop.service.SupplierService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.IntegerStringConverter;
import javafx.util.converter.LongStringConverter;

import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class PurchaseFormController {
    
    @FXML private TextField txtCode;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<Supplier> cbSupplier;
    @FXML private TextField txtNote;
    
    @FXML private TableView<PurchaseItem> itemsTable;
    @FXML private TableColumn<PurchaseItem, String> colProduct;
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

    private ObservableList<PurchaseItem> itemsList = FXCollections.observableArrayList();
    private final NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

    @FXML
    public void initialize() {
        try {
            List<Supplier> suppliers = new com.shop.dao.SupplierDao().findAllActive();
            cbSupplier.setItems(FXCollections.observableArrayList(suppliers));
            
            // Set cell factory to display supplier name
            cbSupplier.setCellFactory(lv -> new ListCell<Supplier>() {
                @Override
                protected void updateItem(Supplier item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.getName() + " - " + item.getPhone());
                }
            });
            cbSupplier.setButtonCell(new ListCell<Supplier>() {
                @Override
                protected void updateItem(Supplier item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.getName() + " - " + item.getPhone());
                }
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }

        datePicker.setValue(LocalDate.now());
        
        setupTable();
        txtPaid.textProperty().addListener((obs, oldV, newV) -> calculateTotals());
    }

    private void setupTable() {
        itemsTable.setItems(itemsList);
        itemsTable.setEditable(true);

        colProduct.setCellValueFactory(new PropertyValueFactory<>("productName"));
        
        colQty.setCellValueFactory(new PropertyValueFactory<>("qty"));
        colQty.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colQty.setOnEditCommit(event -> {
            PurchaseItem item = event.getRowValue();
            item.setQty(event.getNewValue());
            item.setAmount(item.getQty() * item.getCostPrice());
            itemsTable.refresh();
            calculateTotals();
        });

        colPrice.setCellValueFactory(new PropertyValueFactory<>("costPrice"));
        colPrice.setCellFactory(TextFieldTableCell.forTableColumn(new LongStringConverter()));
        colPrice.setOnEditCommit(event -> {
            PurchaseItem item = event.getRowValue();
            item.setCostPrice(event.getNewValue());
            item.setAmount(item.getQty() * item.getCostPrice());
            itemsTable.refresh();
            calculateTotals();
        });

        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colAmount.setCellFactory(tc -> new TableCell<PurchaseItem, Long>() {
            @Override
            protected void updateItem(Long price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : currencyFormat.format(price));
            }
        });

        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button("Xóa");
            {
                deleteBtn.getStyleClass().add("button-danger");
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

    private void calculateTotals() {
        long total = 0;
        for (PurchaseItem item : itemsList) {
            total += item.getAmount();
        }
        lblTotal.setText(currencyFormat.format(total) + " đ");

        long paid = 0;
        try {
            if (!txtPaid.getText().isEmpty()) {
                paid = Long.parseLong(txtPaid.getText().replaceAll("[^0-9]", ""));
            }
        } catch (NumberFormatException ignored) {}

        long debt = total - paid;
        if (debt < 0) debt = 0;
        lblDebt.setText(currencyFormat.format(debt) + " đ");
    }

    @FXML
    public void handleAddLine() {
        try {
            List<Product> products = productService.searchActiveProducts("");
            ChoiceDialog<Product> dialog = new ChoiceDialog<>(null, products);
            dialog.setTitle("Chọn sản phẩm");
            dialog.setHeaderText("Chọn sản phẩm cần nhập hàng");
            dialog.setContentText("Sản phẩm:");
            
            // Custom converter for the ChoiceDialog
            ComboBox<Product> comboBox = (ComboBox<Product>) dialog.getDialogPane().lookup(".combo-box");
            if (comboBox != null) {
                comboBox.setConverter(new javafx.util.StringConverter<Product>() {
                    @Override
                    public String toString(Product object) {
                        return object == null ? "" : object.getName() + " - " + object.getCode();
                    }
                    @Override
                    public Product fromString(String string) { return null; }
                });
            }

            dialog.showAndWait().ifPresent(product -> {
                PurchaseItem item = new PurchaseItem();
                item.setProductId(product.getId());
                item.setProductName(product.getName());
                item.setQty(1);
                item.setCostPrice(0); // user will edit
                item.setAmount(0);
                itemsList.add(item);
                calculateTotals();
            });
        } catch (SQLException e) {
            showAlert("Lỗi", "Không thể tải danh sách sản phẩm: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void handleSave() {
        if (cbSupplier.getValue() == null) {
            showAlert("Lỗi", "Vui lòng chọn nhà cung cấp.", Alert.AlertType.ERROR);
            return;
        }
        if (itemsList.isEmpty()) {
            showAlert("Lỗi", "Vui lòng chọn ít nhất 1 sản phẩm.", Alert.AlertType.ERROR);
            return;
        }

        Purchase purchase = new Purchase();
        purchase.setCode("PN-" + System.currentTimeMillis());
        purchase.setSupplierId(cbSupplier.getValue().getId());
        if (datePicker.getValue() != null) {
            purchase.setPurchaseDate(datePicker.getValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        } else {
            purchase.setPurchaseDate(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        purchase.setNote(txtNote.getText());

        long paid = 0;
        try {
            if (!txtPaid.getText().isEmpty()) {
                paid = Long.parseLong(txtPaid.getText().replaceAll("[^0-9]", ""));
            }
        } catch (NumberFormatException ignored) {}
        purchase.setPaid(paid);

        try {
            purchaseService.createPurchase(purchase, itemsList);
            showAlert("Thành công", "Lưu phiếu nhập thành công!", Alert.AlertType.INFORMATION);
            handleBack();
        } catch (Exception e) {
            showAlert("Lỗi", "Lỗi khi lưu phiếu nhập: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void handleBack() {
        javafx.stage.Stage stage = null;
        if (lblTotal != null && lblTotal.getScene() != null) {
            stage = (javafx.stage.Stage) lblTotal.getScene().getWindow();
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
