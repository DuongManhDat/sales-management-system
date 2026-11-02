package com.shop.controller;

import com.shop.dao.InvoiceDao;
import com.shop.model.Customer;
import com.shop.model.Invoice;
import com.shop.control.PaginationControl;
import com.shop.model.common.PageResult;
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
    @FXML private PaginationControl paginationControl;
    private int currentInvoicePage = 1;
    private static final int INVOICE_PAGE_SIZE = 7;

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
        paginationControl.setPageSize(INVOICE_PAGE_SIZE);
        paginationControl.setOnPageChangeListener(page -> {
            this.currentInvoicePage = page;
            loadInvoices();
        });
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
        colInvoiceStatus.setCellFactory(com.shop.util.StatusBadge.createCellFactory());
        
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
        boolean active = currentCustomer.isActive();
        lblStatus.setText(active ? "Hoạt động" : "Ngừng hoạt động");
        lblStatus.getStyleClass().removeAll("badge-status-active", "badge-status-inactive", "badge-status-danger");
        if (active) {
            lblStatus.getStyleClass().add("badge-status-active");
        } else {
            lblStatus.getStyleClass().add("badge-status-inactive");
        }
    }

    private void loadInvoices() {
        Task<PageResult<Invoice>> loadTask = new Task<PageResult<Invoice>>() {
            @Override
            protected PageResult<Invoice> call() throws Exception {
                return invoiceDao.findPagedByCustomerId(currentCustomer.getId(), currentInvoicePage, INVOICE_PAGE_SIZE);
            }
        };
        
        loadTask.setOnSucceeded(e -> {
            PageResult<Invoice> result = loadTask.getValue();
            invoiceList.setAll(result.getItems());
            paginationControl.updateState(result.getPageIndex(), result.getPageSize(), result.getTotalItems());
            
            // Tính tổng doanh số và nợ từ tất cả hóa đơn của khách
            try {
                List<Invoice> allInvoices = invoiceDao.findByCustomerId(currentCustomer.getId());
                long totalSales = allInvoices.stream().mapToLong(Invoice::getTotal).sum();
                long totalDebt = allInvoices.stream().mapToLong(Invoice::getDebt).sum();
                lblTotalSales.setText(String.format("%,d đ", totalSales));
                lblTotalDebt.setText(String.format("%,d đ", totalDebt));
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
        
        loadTask.setOnFailed(e -> {
            e.getSource().getException().printStackTrace();
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
