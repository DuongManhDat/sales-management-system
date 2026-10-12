package com.shop.controller;

import com.shop.dao.InvoiceDao;
import com.shop.model.Invoice;
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

public class InvoiceListController {
    private static final Logger log = LoggerFactory.getLogger(InvoiceListController.class);
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,###");

    @FXML private TextField searchField;
    @FXML private DatePicker dateFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TableView<Invoice> invoiceTable;
    @FXML private TableColumn<Invoice, String> colCode;
    @FXML private TableColumn<Invoice, String> colDate;
    @FXML private TableColumn<Invoice, String> colCustomer;
    @FXML private TableColumn<Invoice, Number> colTotal;
    @FXML private TableColumn<Invoice, Number> colPaid;
    @FXML private TableColumn<Invoice, Number> colDebt;
    @FXML private TableColumn<Invoice, String> colStatus;

    private final InvoiceDao invoiceDao = new InvoiceDao();

    @FXML
    public void initialize() {
        statusFilter.setItems(FXCollections.observableArrayList("Tất cả", "Đã thanh toán", "Còn nợ"));
        statusFilter.getSelectionModel().selectFirst();

        setupDatePickerConverter();

        colCode.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCode()));
        colDate.setCellValueFactory(data -> new SimpleStringProperty(
                FormatterUtil.formatDateToDdMmYyyy(data.getValue().getInvoiceDate())));

        colCustomer.setCellValueFactory(data -> {
            String name = data.getValue().getCustomerName();
            if (name != null && !name.trim().isEmpty()) {
                return new SimpleStringProperty(name);
            }
            return new SimpleStringProperty("KH #" + data.getValue().getCustomerId());
        });

        colTotal.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().getTotal()));
        colPaid.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().getPaid()));
        colDebt.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().getDebt()));

        // Format tiền tệ chia đơn vị và kết thúc bằng đ
        colTotal.setCellFactory(col -> createCurrencyCell());
        colPaid.setCellFactory(col -> createCurrencyCell());
        colDebt.setCellFactory(col -> createCurrencyCell());

        colStatus.setCellValueFactory(data -> new SimpleStringProperty(
                FormatterUtil.formatInvoiceStatus(data.getValue())));
        colStatus.setCellFactory(column -> new TableCell<Invoice, String>() {
            private final Label badge = new Label();
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.trim().isEmpty()) {
                    setGraphic(null);
                    setText(null);
                } else {
                    badge.setText(item);
                    if ("Đã thanh toán".equalsIgnoreCase(item) || "Đã hoàn thành".equalsIgnoreCase(item)) {
                        badge.setStyle("-fx-background-color: #ECFDF5; -fx-text-fill: #047857; -fx-padding: 3 10; -fx-background-radius: 4; -fx-font-weight: bold; -fx-font-size: 12px;");
                    } else if ("Còn nợ".equalsIgnoreCase(item)) {
                        badge.setStyle("-fx-background-color: #FEF2F2; -fx-text-fill: #B91C1C; -fx-padding: 3 10; -fx-background-radius: 4; -fx-font-weight: bold; -fx-font-size: 12px;");
                    } else if ("Đang xử lý".equalsIgnoreCase(item)) {
                        badge.setStyle("-fx-background-color: #EFF6FF; -fx-text-fill: #1D4ED8; -fx-padding: 3 10; -fx-background-radius: 4; -fx-font-weight: bold; -fx-font-size: 12px;");
                    } else if ("Đã hủy".equalsIgnoreCase(item)) {
                        badge.setStyle("-fx-background-color: #F3F4F6; -fx-text-fill: #4B5563; -fx-padding: 3 10; -fx-background-radius: 4; -fx-font-weight: bold; -fx-font-size: 12px;");
                    } else {
                        badge.setStyle("-fx-background-color: #F3F4F6; -fx-text-fill: #374151; -fx-padding: 3 10; -fx-background-radius: 4; -fx-font-weight: bold; -fx-font-size: 12px;");
                    }
                    setGraphic(badge);
                    setText(null);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });

        // Double-click mở dialog lịch sử thanh toán
        invoiceTable.setRowFactory(tv -> {
            TableRow<Invoice> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    openPaymentHistoryDialog(row.getItem());
                }
            });
            return row;
        });

        searchField.textProperty().addListener((obs, old, val) -> loadInvoices());
        statusFilter.valueProperty().addListener((obs, old, val) -> loadInvoices());
        dateFilter.valueProperty().addListener((obs, old, val) -> loadInvoices());

        loadInvoices();
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
    private void handleOpenPaymentHistory() {
        Invoice selected = invoiceTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Chưa chọn hóa đơn");
            alert.setHeaderText(null);
            alert.setContentText("Vui lòng chọn một hóa đơn từ bảng để xem lịch sử thanh toán hoặc thu nợ.");
            alert.showAndWait();
            return;
        }
        openPaymentHistoryDialog(selected);
    }

    private void openPaymentHistoryDialog(Invoice invoice) {
        if (invoice == null) return;
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/invoice-payment-history-dialog.fxml"));
            javafx.scene.Parent root = loader.load();
            InvoicePaymentHistoryController controller = loader.getController();
            controller.setInvoice(invoice);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Lịch sử thanh toán & Thu nợ - " + invoice.getCode());
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            if (invoiceTable.getScene() != null) {
                stage.initOwner(invoiceTable.getScene().getWindow());
            }
            stage.setScene(new javafx.scene.Scene(root));
            stage.showAndWait();

            // Nếu có thanh toán mới được ghi nhận, tải lại danh sách hóa đơn
            if (controller.isPaymentRecorded()) {
                loadInvoices();
            }
        } catch (Exception e) {
            log.error("Lỗi khi mở dialog lịch sử thanh toán hóa đơn: {}", e.getMessage(), e);
        }
    }

    @FXML
    private void loadInvoices() {
        try {
            String keyword = searchField.getText();
            String filterUI = statusFilter.getValue();
            String dateParam = null;
            if (dateFilter.getValue() != null) {
                dateParam = dateFilter.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE);
            }

            List<Invoice> invoices = invoiceDao.searchInvoices(keyword, filterUI, dateParam);
            invoiceTable.setItems(FXCollections.observableArrayList(invoices));
        } catch (SQLException e) {
            log.error("Failed to load invoices", e);
        }
    }
}
