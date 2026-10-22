package com.shop.controller;

import com.shop.dao.InvoiceDao;
import com.shop.model.Customer;
import com.shop.model.Invoice;
import com.shop.util.FormatterUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

public class CustomerDetailController {

    @FXML private Label lblCustomerName;
    @FXML private Label lblCode;
    @FXML private Label lblPhone;
    @FXML private Label lblEmail;
    @FXML private Label lblDateOfBirth;
    @FXML private Label lblGender;
    @FXML private Label lblAddress;
    @FXML private Label lblStatus;
    @FXML private Label lblNote;
    
    @FXML private Label lblTotalSales;
    @FXML private Label lblTotalDebt;

    @FXML private TableView<Invoice> invoiceTable;
    @FXML private Button btnPaymentHistory;
    @FXML private TableColumn<Invoice, String> colInvoiceCode;
    @FXML private TableColumn<Invoice, String> colInvoiceDate;
    @FXML private TableColumn<Invoice, Long> colTotal;
    @FXML private TableColumn<Invoice, Long> colPaid;
    @FXML private TableColumn<Invoice, Long> colDebt;
    @FXML private TableColumn<Invoice, String> colInvoiceStatus;

    private Customer currentCustomer;
    private InvoiceDao invoiceDao;
    private ObservableList<Invoice> invoiceList;
    private Runnable onBackAction;
    private Runnable onEditAction;

    @FXML
    public void initialize() {
        invoiceDao = new InvoiceDao();
        invoiceList = FXCollections.observableArrayList();
        
        setupTable();
    }
    
    public void initData(Customer customer, Runnable onBack, Runnable onEdit) {
        this.currentCustomer = customer;
        this.onBackAction = onBack;
        this.onEditAction = onEdit;
        
        populateCustomerInfo();
        loadInvoices();
    }

    private void setupTable() {
        colInvoiceCode.setCellValueFactory(new PropertyValueFactory<>("code"));
        colInvoiceDate.setCellValueFactory(data -> new SimpleStringProperty(
                FormatterUtil.formatDateToDdMmYyyy(data.getValue().getInvoiceDate())
        ));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colPaid.setCellValueFactory(new PropertyValueFactory<>("paid"));
        colDebt.setCellValueFactory(new PropertyValueFactory<>("debt"));

        colTotal.setCellFactory(column -> new TableCell<Invoice, Long>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%,d đ", item));
                }
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        });

        colPaid.setCellFactory(column -> new TableCell<Invoice, Long>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%,d đ", item));
                }
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        });

        colDebt.setCellFactory(column -> new TableCell<Invoice, Long>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%,d đ", item));
                }
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        });

        colInvoiceStatus.setCellValueFactory(data -> new SimpleStringProperty(FormatterUtil.formatInvoiceStatus(data.getValue())));
        colInvoiceStatus.setCellFactory(column -> new TableCell<Invoice, String>() {
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
        
        invoiceTable.setItems(invoiceList);

        // Nút Lịch sử & Thu nợ chỉ enable khi có bản ghi hóa đơn được chọn
        btnPaymentHistory.disableProperty().bind(
                invoiceTable.getSelectionModel().selectedItemProperty().isNull()
        );

        // Double click vào hóa đơn để xem chi tiết hóa đơn
        invoiceTable.setRowFactory(tv -> {
            TableRow<Invoice> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    openInvoiceDetailDialog(row.getItem());
                }
            });
            return row;
        });
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

            Stage stage = new Stage();
            stage.setTitle("Chi tiết hóa đơn - " + invoice.getCode());
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            if (invoiceTable.getScene() != null) {
                stage.initOwner(invoiceTable.getScene().getWindow());
            }
            stage.setScene(new javafx.scene.Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openPaymentHistoryDialog(Invoice invoice) {
        if (invoice == null) return;
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/invoice-payment-history-dialog.fxml"));
            javafx.scene.Parent root = loader.load();
            InvoicePaymentHistoryController controller = loader.getController();
            controller.setInvoice(invoice);

            Stage stage = new Stage();
            stage.setTitle("Lịch sử thanh toán & Thu nợ - " + invoice.getCode());
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            if (invoiceTable.getScene() != null) {
                stage.initOwner(invoiceTable.getScene().getWindow());
            }
            stage.setScene(new javafx.scene.Scene(root));
            stage.showAndWait();

            // Nếu đã thực hiện thu nợ, tải lại danh sách hóa đơn và tổng nợ của khách hàng
            if (controller.isPaymentRecorded()) {
                loadInvoices();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void populateCustomerInfo() {
        if (currentCustomer == null) return;
        
        lblCustomerName.setText(currentCustomer.getName());
        lblCode.setText(currentCustomer.getCode());
        lblPhone.setText(currentCustomer.getPhone());
        lblEmail.setText(currentCustomer.getEmail() != null ? currentCustomer.getEmail() : "-");
        if (currentCustomer.getDateOfBirth() != null) {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy");
            lblDateOfBirth.setText(currentCustomer.getDateOfBirth().format(formatter));
        } else {
            lblDateOfBirth.setText("-");
        }
        
        String genderStr = "-";
        if (currentCustomer.getGender() != null) {
            switch (currentCustomer.getGender()) {
                case MALE: genderStr = "Nam"; break;
                case FEMALE: genderStr = "Nữ"; break;
            }
        }
        lblGender.setText(genderStr);
        lblAddress.setText(currentCustomer.getAddress() != null ? currentCustomer.getAddress() : "-");
        lblNote.setText(currentCustomer.getNote() != null ? currentCustomer.getNote() : "-");
        lblStatus.setText(currentCustomer.isActive() ? "Hoạt động" : "Ngừng hoạt động");
    }

    private void loadInvoices() {
        Task<List<Invoice>> loadTask = new Task<List<Invoice>>() {
            @Override
            protected List<Invoice> call() throws Exception {
                return invoiceDao.findByCustomerId(currentCustomer.getId());
            }
        };
        
        loadTask.setOnSucceeded(e -> {
            List<Invoice> invoices = loadTask.getValue();
            invoiceList.setAll(invoices);
            
            long totalSales = invoices.stream().mapToLong(Invoice::getTotal).sum();
            long totalDebt = invoices.stream().mapToLong(Invoice::getDebt).sum();
            
            lblTotalSales.setText(String.format("%,d đ", totalSales));
            lblTotalDebt.setText(String.format("%,d đ", totalDebt));
        });
        
        loadTask.setOnFailed(e -> {
            e.getSource().getException().printStackTrace();
            // Handle error silently or show alert
        });
        
        new Thread(loadTask).start();
    }

    @FXML
    private void handleBack() {
        if (onBackAction != null) onBackAction.run();
    }

    @FXML
    private void handleEdit() {
        if (onEditAction != null) onEditAction.run();
    }
}
