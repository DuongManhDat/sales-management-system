package com.shop.controller;

import com.shop.dao.InvoiceDao;
import com.shop.model.Invoice;
import com.shop.util.FormatterUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class InvoiceListController {
    private static final Logger log = LoggerFactory.getLogger(InvoiceListController.class);

    @FXML private TextField searchField;
    @FXML private DatePicker dateFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Button btnPaymentHistory;
    @FXML private TableView<Invoice> invoiceTable;
    @FXML private TableColumn<Invoice, String> colCode;
    @FXML private TableColumn<Invoice, String> colDate;
    @FXML private TableColumn<Invoice, String> colCustomer;
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

        colStatus.setCellValueFactory(data -> new SimpleStringProperty(
                FormatterUtil.formatInvoiceStatus(data.getValue())));
        colStatus.setCellFactory(com.shop.util.StatusBadge.createCellFactory());

        // Nút Lịch sử & Thu nợ chỉ enable khi có bản ghi được chọn
        btnPaymentHistory.disableProperty().bind(
                invoiceTable.getSelectionModel().selectedItemProperty().isNull()
        );

        // Double-click mở dialog chi tiết hóa đơn
        invoiceTable.setRowFactory(tv -> {
            TableRow<Invoice> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    openInvoiceDetailDialog(row.getItem());
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

    private void openInvoiceDetailDialog(Invoice invoice) {
        if (invoice == null) return;
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/invoice-detail-dialog.fxml"));
            javafx.scene.Parent root = loader.load();
            InvoiceDetailController controller = loader.getController();
            controller.setInvoice(invoice);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Chi tiết hóa đơn - " + invoice.getCode());
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            if (invoiceTable.getScene() != null) {
                stage.initOwner(invoiceTable.getScene().getWindow());
            }
            stage.setScene(new javafx.scene.Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            log.error("Lỗi khi mở dialog chi tiết hóa đơn: {}", e.getMessage(), e);
        }
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
