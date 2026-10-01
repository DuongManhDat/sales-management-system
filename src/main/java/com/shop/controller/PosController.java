package com.shop.controller;

import com.shop.dao.CustomerDao;
import com.shop.dao.ProductDao;
import com.shop.model.Customer;
import com.shop.model.Invoice;
import com.shop.model.InvoiceItem;
import com.shop.model.Product;
import com.shop.service.CustomerService;
import com.shop.service.SalesService;
import com.shop.util.FormatterUtil;
import com.shop.viewmodel.PosViewModel;
import javafx.application.Platform;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.List;

public class PosController {
    private static final Logger log = LoggerFactory.getLogger(PosController.class);
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,###");

    // Left Panel - Cart & Table
    @FXML private Label lblInvoiceMeta;
    @FXML private Button btnClearCart;
    @FXML private TableView<InvoiceItem> cartTable;
    @FXML private TableColumn<InvoiceItem, Number> colIndex;
    @FXML private TableColumn<InvoiceItem, Void> colDelete;
    @FXML private TableColumn<InvoiceItem, String> colProductCode;
    @FXML private TableColumn<InvoiceItem, String> colProductName;
    @FXML private TableColumn<InvoiceItem, Number> colPrice;
    @FXML private TableColumn<InvoiceItem, Void> colQty;
    @FXML private TableColumn<InvoiceItem, Number> colAmount;

    @FXML private Label lblItemCount;
    @FXML private Label lblTotalQty;
    @FXML private Label lblSubtotalFooter;

    // Right Panel - Customer, Search, Billing
    @FXML private ComboBox<Customer> customerComboBox;
    @FXML private Button btnQuickAddCustomer;
    @FXML private Button btnClearCustomer;
    @FXML private TextField searchProductField;
    @FXML private ListView<Product> productListView;

    @FXML private Label lblSubtotal;
    @FXML private TextField txtDiscount;
    @FXML private Label lblTotal;
    @FXML private TextField txtCustomerPaid;
    @FXML private Label lblChangeTitle;
    @FXML private Label lblChange;
    @FXML private Button btnCheckout;

    private final PosViewModel viewModel = new PosViewModel();
    private final ProductDao productDao = new ProductDao();
    private final CustomerDao customerDao = new CustomerDao();
    private final CustomerService customerService = new CustomerService();
    private final SalesService salesService = new SalesService();

    @FXML
    public void initialize() {
        setupCartTable();
        setupProductSearchAndList();
        setupCustomerBox();
        bindViewModel();
        setupKeyShortcuts();
    }

    private void setupCartTable() {
        cartTable.setItems(viewModel.getInvoiceItems());

        // 1. Column Index (#) - Căn giữa toàn bộ
        colIndex.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                if (empty) {
                    setText(null);
                } else {
                    setText(String.valueOf(getIndex() + 1));
                }
            }
        });

        // 2. Column Delete - Căn giữa toàn bộ
        colDelete.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button();
            {
                btn.getStyleClass().add("btn-delete-row");
                FontIcon icon = new FontIcon("fth-x");
                icon.setIconSize(13);
                btn.setGraphic(icon);
                btn.setOnAction(e -> {
                    InvoiceItem item = getTableView().getItems().get(getIndex());
                    viewModel.removeProduct(item);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                setGraphic(empty ? null : btn);
            }
        });

        // 3. Product Code - Căn giữa
        colProductCode.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getProductCode() != null ? cellData.getValue().getProductCode() : "SP" + cellData.getValue().getProductId()
        ));
        colProductCode.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                setText(empty || item == null ? null : item);
            }
        });

        // 4. Product Name - Căn giữa theo chiều dọc (CENTER_LEFT)
        colProductName.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getProductName() != null ? cellData.getValue().getProductName() : "Sản phẩm #" + cellData.getValue().getProductId()
        ));
        colProductName.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER_LEFT);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    InvoiceItem rowItem = getTableView().getItems().get(getIndex());
                    HBox line = new HBox(6);
                    line.setAlignment(Pos.CENTER_LEFT);

                    Label nameLbl = new Label(item);
                    nameLbl.getStyleClass().add("text-bold");

                    line.getChildren().add(nameLbl);

                    if (rowItem.getUnitName() != null && !rowItem.getUnitName().isBlank()) {
                        Label unitLbl = new Label("(" + rowItem.getUnitName() + ")");
                        unitLbl.getStyleClass().add("text-muted");
                        unitLbl.setStyle("-fx-font-size: 11px;");
                        line.getChildren().add(unitLbl);
                    }

                    setGraphic(line);
                }
            }
        });

        // 5. Price - Căn giữa chiều dọc, sát phải (CENTER_RIGHT)
        colPrice.setCellValueFactory(cellData -> new SimpleLongProperty(cellData.getValue().getSalePrice()));
        colPrice.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER_RIGHT);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(CURRENCY_FORMAT.format(item.longValue()));
                }
            }
        });

        // 6. Qty (+ / - controller) - Căn giữa hoàn toàn
        colQty.setCellFactory(col -> new TableCell<>() {
            private final Button btnMinus = new Button("-");
            private final TextField txtQty = new TextField();
            private final Button btnPlus = new Button("+");
            private final HBox container = new HBox(2, btnMinus, txtQty, btnPlus);

            {
                container.setAlignment(Pos.CENTER);
                container.getStyleClass().add("qty-control-box");
                btnMinus.getStyleClass().add("qty-btn");
                btnPlus.getStyleClass().add("qty-btn");
                txtQty.getStyleClass().add("qty-field");

                btnMinus.setOnAction(e -> {
                    InvoiceItem rowItem = getTableView().getItems().get(getIndex());
                    viewModel.decreaseQty(rowItem);
                    cartTable.refresh();
                });

                btnPlus.setOnAction(e -> {
                    InvoiceItem rowItem = getTableView().getItems().get(getIndex());
                    viewModel.increaseQty(rowItem);
                    cartTable.refresh();
                });

                txtQty.setOnAction(e -> applyQty());
                txtQty.focusedProperty().addListener((obs, oldV, newV) -> {
                    if (!newV) applyQty();
                });
            }

            private void applyQty() {
                if (getIndex() < 0 || getIndex() >= getTableView().getItems().size()) return;
                InvoiceItem rowItem = getTableView().getItems().get(getIndex());
                try {
                    int val = Integer.parseInt(txtQty.getText().trim());
                    if (val > 0) {
                        viewModel.updateQty(rowItem, val);
                    } else {
                        viewModel.removeProduct(rowItem);
                    }
                } catch (NumberFormatException ex) {
                    txtQty.setText(String.valueOf(rowItem.getQty()));
                }
                cartTable.refresh();
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                if (empty) {
                    setGraphic(null);
                } else {
                    InvoiceItem rowItem = getTableView().getItems().get(getIndex());
                    txtQty.setText(String.valueOf(rowItem.getQty()));
                    setGraphic(container);
                }
            }
        });

        // 7. Amount (Thành tiền) - Căn giữa chiều dọc, sát phải (CENTER_RIGHT)
        colAmount.setCellValueFactory(cellData -> new SimpleLongProperty(cellData.getValue().getAmount()));
        colAmount.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER_RIGHT);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(CURRENCY_FORMAT.format(item.longValue()) + " đ");
                }
            }
        });
    }

    private void setupProductSearchAndList() {
        // Chỉ tìm kiếm và hiển thị sản phẩm khi người dùng nhập từ khóa vào ô search
        searchProductField.textProperty().addListener((obs, old, val) -> {
            refreshProductList();
        });

        // Placeholder hướng dẫn khi chưa tìm kiếm
        Label emptyPlaceholder = new Label("Gõ mã hoặc tên sản phẩm để tìm kiếm");
        emptyPlaceholder.getStyleClass().add("text-muted");
        productListView.setPlaceholder(emptyPlaceholder);

        // Custom Cell for Product List
        productListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox row = new HBox(8);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.getStyleClass().add("product-cell-box");

                    VBox info = new VBox(2);
                    HBox titleBox = new HBox(6);
                    titleBox.setAlignment(Pos.CENTER_LEFT);

                    Label codeLbl = new Label(item.getCode());
                    codeLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #2563EB; -fx-font-size: 12px;");

                    Label nameLbl = new Label(item.getName());
                    nameLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #0F172A; -fx-font-size: 13px;");

                    titleBox.getChildren().addAll(codeLbl, nameLbl);

                    Label catUnitLbl = new Label((item.getCategoryName() != null ? item.getCategoryName() : "Chưa phân loại")
                            + " • " + (item.getUnitName() != null ? item.getUnitName() : "Đơn vị"));
                    catUnitLbl.getStyleClass().add("text-muted");
                    catUnitLbl.setStyle("-fx-font-size: 11px;");

                    info.getChildren().addAll(titleBox, catUnitLbl);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    VBox priceStockBox = new VBox(2);
                    priceStockBox.setAlignment(Pos.CENTER_RIGHT);

                    Label priceLbl = new Label(CURRENCY_FORMAT.format(item.getSalePrice()) + " đ");
                    priceLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #0F172A; -fx-font-size: 13px;");

                    Label stockLbl = new Label("Tồn: " + (int) item.getStockQty());
                    stockLbl.getStyleClass().add(item.getStockQty() > 0 ? "stock-tag-ok" : "stock-tag-warn");

                    priceStockBox.getChildren().addAll(priceLbl, stockLbl);

                    row.getChildren().addAll(info, spacer, priceStockBox);
                    setGraphic(row);
                }
            }
        });

        productListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1 || event.getClickCount() == 2) {
                Product selected = productListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    viewModel.addProduct(selected);
                    cartTable.refresh();
                }
            }
        });

        // Mặc định ban đầu giỏ tìm kiếm trống, chỉ hiện khi người dùng gõ
        productListView.setItems(FXCollections.observableArrayList());
    }

    private void refreshProductList() {
        String keyword = searchProductField.getText() != null ? searchProductField.getText().trim() : "";
        if (keyword.isEmpty()) {
            productListView.setItems(FXCollections.observableArrayList());
            return;
        }

        try {
            List<Product> products = productDao.searchProducts(keyword);
            productListView.setItems(FXCollections.observableArrayList(products));
        } catch (SQLException e) {
            log.error("Failed to search products", e);
        }
    }

    private void setupCustomerBox() {
        loadCustomers();

        customerComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Customer c) {
                if (c == null) return "";
                return c.getName() + " - " + (c.getPhone() != null ? c.getPhone() : "Chưa có SĐT");
            }
            @Override
            public Customer fromString(String string) {
                return null;
            }
        });

        customerComboBox.valueProperty().bindBidirectional(viewModel.selectedCustomerProperty());
    }

    private void loadCustomers() {
        try {
            List<Customer> customers = customerDao.getAllActive();
            customerComboBox.setItems(FXCollections.observableArrayList(customers));
        } catch (SQLException e) {
            log.error("Failed to load customers", e);
        }
    }

    @FXML
    private void handleClearCustomer() {
        customerComboBox.setValue(null);
    }

    @FXML
    private void handleQuickAddCustomer() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/customer-form-dialog.fxml"));
            Parent root = loader.load();
            CustomerFormController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Thêm khách hàng mới");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));

            controller.initData(new Customer(), customerService, () -> {
                loadCustomers();
                try {
                    List<Customer> list = customerDao.getAllActive();
                    if (!list.isEmpty()) {
                        customerComboBox.setValue(list.get(list.size() - 1));
                    }
                } catch (SQLException ex) {
                    log.error("Error setting newly added customer", ex);
                }
            });

            stage.showAndWait();
        } catch (IOException e) {
            log.error("Failed to open CustomerFormDialog", e);
            showAlert("Lỗi", "Không thể mở hộp thoại thêm khách hàng: " + e.getMessage());
        }
    }

    private void bindViewModel() {
        // Subtotal & Totals
        viewModel.subtotalProperty().addListener((obs, old, val) -> {
            String formatted = CURRENCY_FORMAT.format(val.longValue()) + " đ";
            lblSubtotal.setText(formatted);
            lblSubtotalFooter.setText(formatted);
        });

        viewModel.totalProperty().addListener((obs, old, val) -> {
            lblTotal.setText(CURRENCY_FORMAT.format(val.longValue()) + " đ");
            updateChangeOrDebt(viewModel.changeOrDebtProperty().get());
        });

        viewModel.totalQuantityProperty().addListener((obs, old, val) -> {
            lblTotalQty.setText(String.valueOf(val.intValue()));
        });

        viewModel.totalItemCountProperty().addListener((obs, old, val) -> {
            lblItemCount.setText(String.valueOf(val.intValue()));
        });

        viewModel.changeOrDebtProperty().addListener((obs, old, val) -> {
            updateChangeOrDebt(val.longValue());
        });

        // Attach Currency Formatter (định dạng dấu chấm phân cách hàng nghìn khi nhập)
        FormatterUtil.attachCurrencyFormatter(txtDiscount, () -> {
            long discount = FormatterUtil.parseNumberFromText(txtDiscount.getText());
            viewModel.discountAmtProperty().set(discount);
        });

        FormatterUtil.attachCurrencyFormatter(txtCustomerPaid, () -> {
            long paid = FormatterUtil.parseNumberFromText(txtCustomerPaid.getText());
            viewModel.customerPaidProperty().set(paid);
        });
    }

    private void updateChangeOrDebt(long diff) {
        if (diff >= 0) {
            lblChangeTitle.setText("Tiền thừa trả khách:");
            lblChange.setText(CURRENCY_FORMAT.format(diff) + " đ");
            lblChange.setStyle("-fx-text-fill: #16A34A; -fx-font-weight: bold; -fx-font-size: 16px;");
        } else {
            lblChangeTitle.setText("Còn nợ (ghi nợ KH):");
            lblChange.setText(CURRENCY_FORMAT.format(Math.abs(diff)) + " đ");
            lblChange.setStyle("-fx-text-fill: #EA580C; -fx-font-weight: bold; -fx-font-size: 16px;");
        }
    }

    @FXML
    private void handleClearCart() {
        if (viewModel.getInvoiceItems().isEmpty()) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Bạn có chắc muốn xóa toàn bộ sản phẩm trong giỏ hàng?", ButtonType.YES, ButtonType.NO);
        alert.setHeaderText("Xác nhận làm trống giỏ hàng");
        alert.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                viewModel.clearCart();
                txtDiscount.setText("0");
                txtCustomerPaid.setText("0");
            }
        });
    }

    @FXML
    private void handleCheckout() {
        if (viewModel.getInvoiceItems().isEmpty()) {
            showAlert("Thông báo", "Giỏ hàng đang trống! Vui lòng chọn ít nhất 1 sản phẩm.");
            return;
        }

        Invoice invoice = new Invoice();
        String invoiceCode = "HD" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        String invoiceDate = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        invoice.setCode(invoiceCode);
        invoice.setInvoiceDate(invoiceDate);

        Customer customer = customerComboBox.getValue();
        if (customer != null) {
            invoice.setCustomerId(customer.getId());
        }
        invoice.setSubtotal(viewModel.subtotalProperty().get());
        invoice.setDiscountAmt(viewModel.discountAmtProperty().get());
        invoice.setDiscountPct(0);
        invoice.setTotal(viewModel.totalProperty().get());

        long paid = viewModel.customerPaidProperty().get();
        if (paid < invoice.getTotal()) {
            if (customer == null) {
                showAlert("Yêu cầu khách hàng", "Khách hàng còn nợ (" + CURRENCY_FORMAT.format(invoice.getTotal() - paid) + " đ). Vui lòng chọn khách hàng để ghi nợ!");
                customerComboBox.requestFocus();
                return;
            }
            invoice.setStatus("PARTIAL");
        } else {
            invoice.setStatus("PAID");
        }

        invoice.setPaid(paid);
        invoice.setDebt(Math.max(0, invoice.getTotal() - paid));

        try {
            salesService.createInvoice(invoice, viewModel.getInvoiceItems());
            showAlert("Thành công", "Thanh toán hóa đơn thành công! (Mã HĐ: " + invoice.getCode() + ")");
            viewModel.clearCart();
            txtDiscount.setText("0");
            txtCustomerPaid.setText("0");
            customerComboBox.setValue(null);
            refreshProductList();
        } catch (Exception e) {
            log.error("Lỗi khi thanh toán đơn hàng", e);
            showAlert("Lỗi thanh toán", "Có lỗi xảy ra trong quá trình thanh toán: " + e.getMessage());
        }
    }

    private void setupKeyShortcuts() {
        Platform.runLater(() -> {
            if (cartTable.getScene() != null) {
                cartTable.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.F2) {
                        searchProductField.requestFocus();
                        searchProductField.selectAll();
                        event.consume();
                    } else if (event.getCode() == KeyCode.F3) {
                        customerComboBox.requestFocus();
                        event.consume();
                    } else if (event.getCode() == KeyCode.F9) {
                        handleCheckout();
                        event.consume();
                    }
                });
            }
        });
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
