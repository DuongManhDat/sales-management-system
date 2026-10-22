package com.shop.controller;

import com.shop.dao.InvoiceDao;
import com.shop.dao.InvoiceItemDao;
import com.shop.model.Invoice;
import com.shop.model.InvoiceItem;
import com.shop.model.ReturnInvoice;
import com.shop.model.ReturnInvoiceItem;
import com.shop.service.SalesService;
import com.shop.util.FormatterUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleLongProperty;
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
import javafx.util.converter.IntegerStringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReturnFormController {
    private static final Logger log = LoggerFactory.getLogger(ReturnFormController.class);
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,###");

    @FXML private TextField txtCode;
    @FXML private TextField txtReturnDate;
    @FXML private ComboBox<Invoice> comboInvoiceSearch;
    @FXML private Label lblCustomer;
    @FXML private Label lblOriginalTotal;
    @FXML private Label lblInvoiceDate;
    @FXML private Label lblInvoiceStatus;

    @FXML private TableView<InvoiceItem> itemTable;
    @FXML private TableColumn<InvoiceItem, String> colProductId;
    @FXML private TableColumn<InvoiceItem, String> colProductName;
    @FXML private TableColumn<InvoiceItem, Number> colQty;
    @FXML private TableColumn<InvoiceItem, Number> colReturnedQty;
    @FXML private TableColumn<InvoiceItem, Number> colRemainingQty;
    @FXML private TableColumn<InvoiceItem, Integer> colReturnQty;
    @FXML private TableColumn<InvoiceItem, Number> colPrice;
    @FXML private TableColumn<InvoiceItem, Number> colAmount;

    @FXML private TextField txtReturnFee;
    @FXML private Label lblGrossRefund;
    @FXML private Label lblTotalRefund;

    private final InvoiceDao invoiceDao = new InvoiceDao();
    private final InvoiceItemDao invoiceItemDao = new InvoiceItemDao();
    private final com.shop.dao.ReturnInvoiceItemDao returnInvoiceItemDao = new com.shop.dao.ReturnInvoiceItemDao();
    private final SalesService salesService = new SalesService();

    private Invoice currentInvoice;
    private final ObservableList<Invoice> masterInvoiceList = FXCollections.observableArrayList();
    private FilteredList<Invoice> filteredInvoiceList;
    private final ObservableList<InvoiceItem> invoiceItemList = FXCollections.observableArrayList();
    private final Map<Integer, Integer> returnQtyMap = new HashMap<>(); // invoiceItemId -> returnQty
    private final Map<Integer, Integer> alreadyReturnedMap = new HashMap<>(); // invoiceItemId -> returnedQty

    @FXML
    public void initialize() {
        LocalDateTime now = LocalDateTime.now();
        txtCode.setText("TH" + now.format(DateTimeFormatter.ofPattern("yyMMddHHmmss")));
        txtReturnDate.setText(now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        setupTable();
        loadAllInvoices();
        setupInvoiceSearch();

        txtReturnFee.textProperty().addListener((obs, old, val) -> calculateRefund());
    }

    private void loadAllInvoices() {
        try {
            List<Invoice> invoices = invoiceDao.findAll();
            masterInvoiceList.setAll(invoices);
        } catch (SQLException e) {
            log.error("Failed to load invoice list for autocomplete", e);
        }
    }

    private void setupInvoiceSearch() {
        filteredInvoiceList = new FilteredList<>(masterInvoiceList, inv -> true);
        comboInvoiceSearch.setItems(filteredInvoiceList);

        // Hiển thị trong danh sách xổ xuống: Mã HĐ - Tên KH - Ngày
        comboInvoiceSearch.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Invoice inv, boolean empty) {
                super.updateItem(inv, empty);
                if (empty || inv == null) {
                    setText(null);
                } else {
                    String cust = inv.getCustomerName() != null && !inv.getCustomerName().trim().isEmpty()
                            ? inv.getCustomerName() : "KH #" + inv.getCustomerId();
                    String date = FormatterUtil.formatDateToDdMmYyyy(inv.getInvoiceDate());
                    setText(inv.getCode() + " | " + cust + " | " + date);
                }
            }
        });

        // Converter hiển thị trên ô nhập liệu
        comboInvoiceSearch.setConverter(new StringConverter<>() {
            @Override
            public String toString(Invoice inv) {
                if (inv == null) return "";
                String cust = inv.getCustomerName() != null && !inv.getCustomerName().trim().isEmpty()
                        ? inv.getCustomerName() : "KH #" + inv.getCustomerId();
                return inv.getCode() + " (" + cust + ")";
            }

            @Override
            public Invoice fromString(String str) {
                if (str == null || str.trim().isEmpty()) return null;
                String clean = str.trim();
                for (Invoice inv : masterInvoiceList) {
                    if (inv.getCode() != null && inv.getCode().equalsIgnoreCase(clean)) {
                        return inv;
                    }
                    String display = inv.getCode() + " (" + (inv.getCustomerName() != null ? inv.getCustomerName() : "KH #" + inv.getCustomerId()) + ")";
                    if (display.equalsIgnoreCase(clean)) {
                        return inv;
                    }
                }
                return null;
            }
        });

        // Lắng nghe gõ text đến đâu gợi ý và lọc đến đấy
        comboInvoiceSearch.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            Invoice selected = comboInvoiceSearch.getSelectionModel().getSelectedItem();
            if (selected != null) {
                String selectedLabel = comboInvoiceSearch.getConverter().toString(selected);
                if (selectedLabel.equalsIgnoreCase(newVal)) {
                    return;
                }
            }

            Platform.runLater(() -> {
                String query = comboInvoiceSearch.getEditor().getText();
                if (query == null || query.trim().isEmpty()) {
                    filteredInvoiceList.setPredicate(inv -> true);
                } else {
                    String lower = query.trim().toLowerCase();
                    filteredInvoiceList.setPredicate(inv -> {
                        boolean matchCode = inv.getCode() != null && inv.getCode().toLowerCase().contains(lower);
                        boolean matchCust = inv.getCustomerName() != null && inv.getCustomerName().toLowerCase().contains(lower);
                        boolean matchId = String.valueOf(inv.getId()).equals(lower);
                        return matchCode || matchCust || matchId;
                    });
                }

                if (comboInvoiceSearch.isFocused() && !filteredInvoiceList.isEmpty()) {
                    comboInvoiceSearch.show();
                }
            });
        });

        // Khi chọn hóa đơn từ danh sách
        comboInvoiceSearch.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectInvoice(newVal);
            }
        });

        // Xử lý phím Enter trên ô tìm kiếm
        comboInvoiceSearch.getEditor().setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (!filteredInvoiceList.isEmpty()) {
                    Invoice target = filteredInvoiceList.get(0);
                    comboInvoiceSearch.getSelectionModel().select(target);
                }
            }
        });
    }

    public void setInitialReturnData(Invoice invoice, List<ReturnInvoiceItem> previousItems, long previousFee) {
        if (invoice == null) return;
        comboInvoiceSearch.getSelectionModel().select(invoice);
        selectInvoice(invoice);

        if (previousItems != null && !previousItems.isEmpty()) {
            returnQtyMap.clear();
            for (ReturnInvoiceItem prev : previousItems) {
                for (InvoiceItem item : invoiceItemList) {
                    if (item.getId() == prev.getInvoiceItemId() || item.getProductId() == prev.getProductId()) {
                        returnQtyMap.put(item.getId(), Math.min(prev.getReturnQty(), item.getQty()));
                        break;
                    }
                }
            }
            itemTable.refresh();
        }

        txtReturnFee.setText(String.valueOf(previousFee));
        calculateRefund();
    }

    private void selectInvoice(Invoice invoice) {
        if (invoice == null) return;
        this.currentInvoice = invoice;

        String customerText = currentInvoice.getCustomerName();
        if (customerText == null || customerText.trim().isEmpty()) {
            customerText = "Khách hàng #" + currentInvoice.getCustomerId();
        }
        lblCustomer.setText(customerText);
        lblOriginalTotal.setText(CURRENCY_FORMAT.format(currentInvoice.getTotal()) + " đ");
        lblInvoiceDate.setText(FormatterUtil.formatDateToDdMmYyyy(currentInvoice.getInvoiceDate()));
        lblInvoiceStatus.setText(FormatterUtil.formatInvoiceStatus(currentInvoice.getStatus()));

        try {
            List<InvoiceItem> items = invoiceItemDao.findByInvoiceId(currentInvoice.getId());
            invoiceItemList.setAll(items);
            returnQtyMap.clear();

            // Lấy thông tin số lượng đã trả ở các phiếu trả trước đó
            alreadyReturnedMap.clear();
            Map<Integer, Integer> returnedMap = returnInvoiceItemDao.getReturnedQtyMapByInvoiceId(currentInvoice.getId());
            alreadyReturnedMap.putAll(returnedMap);

            calculateRefund();
        } catch (SQLException e) {
            log.error("Failed to load invoice items", e);
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể nạp chi tiết hàng hóa của hóa đơn: " + e.getMessage());
        }
    }

    private void setupTable() {
        itemTable.setItems(invoiceItemList);
        itemTable.setEditable(true);

        colProductId.setCellValueFactory(data -> {
            String code = data.getValue().getProductCode();
            if (code != null && !code.trim().isEmpty()) {
                return new SimpleStringProperty(code);
            }
            return new SimpleStringProperty("SP#" + data.getValue().getProductId());
        });

        colProductName.setCellValueFactory(data -> {
            String name = data.getValue().getProductName();
            if (name != null && !name.trim().isEmpty()) {
                return new SimpleStringProperty(name);
            }
            return new SimpleStringProperty("Sản phẩm ID: " + data.getValue().getProductId());
        });

        colQty.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().getQty()));
        colQty.setCellFactory(col -> createNumberCell(false));

        colReturnedQty.setCellValueFactory(data -> {
            int returned = alreadyReturnedMap.getOrDefault(data.getValue().getId(), 0);
            return new SimpleLongProperty(returned);
        });
        colReturnedQty.setCellFactory(col -> createNumberCell(false));

        colRemainingQty.setCellValueFactory(data -> {
            int returned = alreadyReturnedMap.getOrDefault(data.getValue().getId(), 0);
            int remaining = Math.max(0, data.getValue().getQty() - returned);
            return new SimpleLongProperty(remaining);
        });
        colRemainingQty.setCellFactory(col -> createNumberCell(false));

        colPrice.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().getSalePrice()));
        colPrice.setCellFactory(col -> createCurrencyCell());

        colReturnQty.setCellValueFactory(data -> {
            int qty = returnQtyMap.getOrDefault(data.getValue().getId(), 0);
            return new javafx.beans.property.SimpleObjectProperty<>(qty);
        });
        colReturnQty.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter() {
            @Override
            public Integer fromString(String value) {
                try {
                    if (value == null || value.trim().isEmpty()) return 0;
                    return Integer.parseInt(value.trim().replaceAll("[^0-9]", ""));
                } catch (Exception e) {
                    return 0;
                }
            }
        }));
        colReturnQty.setOnEditCommit(event -> {
            InvoiceItem item = event.getRowValue();
            Integer newQty = event.getNewValue();
            if (newQty == null || newQty < 0) {
                newQty = 0;
            }
            int returned = alreadyReturnedMap.getOrDefault(item.getId(), 0);
            int maxAllowed = Math.max(0, item.getQty() - returned);
            if (newQty > maxAllowed) {
                showAlert(Alert.AlertType.WARNING, "Cảnh báo",
                        "Số lượng trả không thể vượt quá số lượng còn lại có thể trả (" + maxAllowed + ")");
                newQty = maxAllowed;
            }
            returnQtyMap.put(item.getId(), newQty);
            itemTable.refresh();
            calculateRefund();
        });

        colAmount.setCellValueFactory(data -> {
            int retQty = returnQtyMap.getOrDefault(data.getValue().getId(), 0);
            long amount = (long) retQty * data.getValue().getSalePrice();
            return new SimpleLongProperty(amount);
        });
        colAmount.setCellFactory(col -> createCurrencyCell());
    }

    private TableCell<InvoiceItem, Number> createCurrencyCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(CURRENCY_FORMAT.format(item.longValue()) + " đ");
                }
                setStyle("-fx-alignment: CENTER_RIGHT;");
            }
        };
    }

    private TableCell<InvoiceItem, Number> createNumberCell(boolean alignRight) {
        return new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(CURRENCY_FORMAT.format(item.longValue()));
                }
                setStyle(alignRight ? "-fx-alignment: CENTER_RIGHT;" : "-fx-alignment: CENTER;");
            }
        };
    }

    private void calculateRefund() {
        if (currentInvoice == null) {
            lblGrossRefund.setText("0 đ");
            lblTotalRefund.setText("0 đ");
            return;
        }
        long grossRefund = 0;
        for (InvoiceItem item : invoiceItemList) {
            int qty = returnQtyMap.getOrDefault(item.getId(), 0);
            grossRefund += (long) qty * item.getSalePrice();
        }

        long fee = 0;
        try {
            String feeText = txtReturnFee.getText();
            if (feeText != null && !feeText.trim().isEmpty()) {
                fee = Long.parseLong(feeText.trim().replaceAll("[^0-9]", ""));
            }
        } catch (NumberFormatException ignored) {}

        long netRefund = Math.max(0, grossRefund - fee);
        lblGrossRefund.setText(CURRENCY_FORMAT.format(grossRefund) + " đ");
        lblTotalRefund.setText(CURRENCY_FORMAT.format(netRefund) + " đ");
    }

    @FXML
    private void submitReturn() {
        if (currentInvoice == null) {
            showAlert(Alert.AlertType.WARNING, "Thông báo", "Vui lòng chọn hoặc tìm kiếm hóa đơn gốc trước khi lưu phiếu");
            return;
        }

        long grossRefund = 0;
        List<ReturnInvoiceItem> returnItems = new ArrayList<>();
        for (InvoiceItem item : invoiceItemList) {
            int qty = returnQtyMap.getOrDefault(item.getId(), 0);
            if (qty > 0) {
                ReturnInvoiceItem riItem = new ReturnInvoiceItem();
                riItem.setInvoiceItemId(item.getId());
                riItem.setProductId(item.getProductId());
                riItem.setReturnQty(qty);
                riItem.setRefundPrice(item.getSalePrice());
                riItem.setAmount((long) qty * item.getSalePrice());
                returnItems.add(riItem);

                grossRefund += riItem.getAmount();
            }
        }

        if (returnItems.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thông báo", "Bạn chưa nhập số lượng trả cho bất kỳ sản phẩm nào");
            return;
        }

        long fee = 0;
        try {
            String feeText = txtReturnFee.getText();
            if (feeText != null && !feeText.trim().isEmpty()) {
                fee = Long.parseLong(feeText.trim().replaceAll("[^0-9]", ""));
            }
        } catch (NumberFormatException ignored) {}

        ReturnInvoice ri = new ReturnInvoice();
        String code = txtCode.getText() != null && !txtCode.getText().trim().isEmpty()
                ? txtCode.getText().trim()
                : "TH" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        ri.setCode(code);
        ri.setInvoiceId(currentInvoice.getId());
        ri.setCustomerId(currentInvoice.getCustomerId());
        ri.setTotalRefund(grossRefund);
        ri.setReturnFee(fee);

        try {
            salesService.processReturn(ri, returnItems);
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Phiếu trả hàng " + ri.getCode() + " đã được tạo thành công!");
            handleClose();
        } catch (Exception e) {
            log.error("Lỗi khi tạo phiếu trả hàng", e);
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tạo phiếu: " + e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = null;
        if (comboInvoiceSearch != null && comboInvoiceSearch.getScene() != null) {
            stage = (Stage) comboInvoiceSearch.getScene().getWindow();
        }
        if (stage != null) {
            stage.close();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
