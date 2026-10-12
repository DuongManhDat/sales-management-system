package com.shop.controller;

import com.shop.dao.InvoiceDao;
import com.shop.model.Invoice;
import com.shop.service.SalesService;
import com.shop.util.FormatterUtil;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class OrderListController {
    private static final Logger log = LoggerFactory.getLogger(OrderListController.class);
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,###");

    @FXML private TextField searchField;
    @FXML private DatePicker dateFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TableView<Invoice> orderTable;
    @FXML private TableColumn<Invoice, String> colCode;
    @FXML private TableColumn<Invoice, String> colDate;
    @FXML private TableColumn<Invoice, String> colDelivery;
    @FXML private TableColumn<Invoice, String> colCustomer;
    @FXML private TableColumn<Invoice, Number> colTotal;
    @FXML private TableColumn<Invoice, Number> colPaid;
    @FXML private TableColumn<Invoice, String> colStatus;

    private final InvoiceDao invoiceDao = new InvoiceDao();
    private final SalesService salesService = new SalesService();

    @FXML
    public void initialize() {
        statusFilter.setItems(FXCollections.observableArrayList("Đang xử lý", "Đã hoàn thành", "Đã hủy"));
        statusFilter.getSelectionModel().selectFirst();

        setupDatePickerConverter();

        colCode.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCode()));
        colDate.setCellValueFactory(d -> new SimpleStringProperty(
                FormatterUtil.formatDateToDdMmYyyy(d.getValue().getInvoiceDate())));
        colDelivery.setCellValueFactory(d -> {
            String date = d.getValue().getDeliveryDate();
            return new SimpleStringProperty(date != null && !date.isEmpty()
                    ? FormatterUtil.formatDateToDdMmYyyy(date) : "—");
        });
        colCustomer.setCellValueFactory(d -> {
            String name = d.getValue().getCustomerName();
            return new SimpleStringProperty(name != null && !name.isEmpty()
                    ? name : "KH#" + d.getValue().getCustomerId());
        });
        colTotal.setCellValueFactory(d -> new SimpleLongProperty(d.getValue().getTotal()));
        colPaid.setCellValueFactory(d -> new SimpleLongProperty(d.getValue().getPaid()));

        // Format tiền tệ chia đơn vị và kết thúc bằng đ
        colTotal.setCellFactory(col -> createCurrencyCell());
        colPaid.setCellFactory(col -> createCurrencyCell());

        colStatus.setCellValueFactory(d -> new SimpleStringProperty(
                FormatterUtil.formatInvoiceStatus(d.getValue().getStatus())));

        searchField.textProperty().addListener((obs, old, val) -> loadOrders());
        dateFilter.valueProperty().addListener((obs, old, val) -> loadOrders());
        statusFilter.valueProperty().addListener((obs, old, val) -> loadOrders());
        loadOrders();
    }

    private void setupDatePickerConverter() {
        dateFilter.setConverter(new StringConverter<LocalDate>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            @Override
            public String toString(LocalDate date) {
                return date != null ? formatter.format(date) : "";
            }
            @Override
            public LocalDate fromString(String string) {
                if (string == null || string.trim().isEmpty()) return null;
                try {
                    return LocalDate.parse(string.trim(), formatter);
                } catch (Exception e) {
                    return null;
                }
            }
        });
    }

    private TableCell<Invoice, Number> createCurrencyCell() {
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

    @FXML
    private void handleClearDate() {
        dateFilter.setValue(null);
    }

    @FXML
    private void loadOrders() {
        try {
            String keyword = searchField.getText();
            String filter = statusFilter.getValue();
            String dbStatus;
            switch (filter) {
                case "Đã hoàn thành": dbStatus = "COMPLETED"; break;
                case "Đã hủy":       dbStatus = "CANCELLED"; break;
                default:             dbStatus = "PENDING";    break;
            }
            String dateParam = null;
            if (dateFilter.getValue() != null) {
                dateParam = dateFilter.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE);
            }
            List<Invoice> orders = invoiceDao.searchOrders(keyword, dbStatus, dateParam);
            orderTable.setItems(FXCollections.observableArrayList(orders));
        } catch (SQLException e) {
            log.error("Lỗi khi tải danh sách đơn đặt hàng", e);
        }
    }

    @FXML
    private void completeOrder() {
        Invoice selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Chưa chọn đơn", "Vui lòng chọn 1 đơn để hoàn thành.");
            return;
        }
        if (!"PENDING".equals(selected.getStatus())) {
            showAlert("Không hợp lệ", "Chỉ hoàn thành đơn đang ở trạng thái 'Đang xử lý'.");
            return;
        }
        try {
            salesService.completeOrder(selected.getId());
            showAlert("Thành công", "Đã hoàn thành đơn " + selected.getCode() + ". Hàng đã xuất kho.");
            loadOrders();
        } catch (IllegalArgumentException e) {
            showAlert("Không hợp lệ", e.getMessage());
        } catch (SQLException e) {
            log.error("Lỗi hoàn thành đơn #{}", selected.getId(), e);
            showAlert("Lỗi hệ thống", "Không thể hoàn thành đơn: " + e.getMessage());
        }
    }

    @FXML
    private void cancelOrder() {
        Invoice selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Chưa chọn đơn", "Vui lòng chọn 1 đơn để hủy.");
            return;
        }
        if (!"PENDING".equals(selected.getStatus())) {
            showAlert("Không hợp lệ", "Chỉ hủy đơn đang ở trạng thái 'Đang xử lý'.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận hủy đơn");
        confirm.setHeaderText("Hủy đơn " + selected.getCode() + "?");
        confirm.setContentText("Tồn kho giữ chỗ sẽ được giải phóng. Thao tác này không thể hoàn tác.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    salesService.cancelOrder(selected.getId());
                    showAlert("Đã hủy", "Đã hủy đơn " + selected.getCode() + ".");
                    loadOrders();
                } catch (IllegalArgumentException e) {
                    showAlert("Không hợp lệ", e.getMessage());
                } catch (SQLException e) {
                    log.error("Lỗi hủy đơn #{}", selected.getId(), e);
                    showAlert("Lỗi hệ thống", "Không thể hủy đơn: " + e.getMessage());
                }
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
