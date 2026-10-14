package com.shop.controller;

import com.shop.dao.InvoiceDao;
import com.shop.dao.PaymentDao;
import com.shop.model.Invoice;
import com.shop.model.Payment;
import com.shop.service.PaymentService;
import com.shop.util.FormatterUtil;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.List;

public class InvoicePaymentHistoryController {
    private static final Logger log = LoggerFactory.getLogger(InvoicePaymentHistoryController.class);
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,###");

    @FXML private Label lblHeaderSubtitle;
    @FXML private Label lblInvoiceStatus;
    @FXML private Label lblTotalAmount;
    @FXML private Label lblPaidAmount;
    @FXML private Label lblDebtAmount;

    @FXML private TableView<Payment> paymentTable;
    @FXML private TableColumn<Payment, Number> colIndex;
    @FXML private TableColumn<Payment, String> colPaymentDate;
    @FXML private TableColumn<Payment, String> colAmount;
    @FXML private TableColumn<Payment, String> colPaymentStatus;
    @FXML private TableColumn<Payment, String> colNote;
    @FXML private TableColumn<Payment, Void> colAction;

    @FXML private VBox boxCollectDebt;
    @FXML private TextField txtPaymentAmount;
    @FXML private TextField txtPaymentNote;
    @FXML private Button btnQuickPayAll;
    @FXML private Button btnConfirmPayment;

    private final PaymentDao paymentDao = new PaymentDao();
    private final InvoiceDao invoiceDao = new InvoiceDao();
    private final PaymentService paymentService = new PaymentService();

    private Invoice currentInvoice;
    private final ObservableList<Payment> paymentList = FXCollections.observableArrayList();
    private boolean paymentRecorded = false;

    @FXML
    public void initialize() {
        colIndex.setCellValueFactory(data -> new SimpleIntegerProperty(paymentList.indexOf(data.getValue()) + 1));
        colPaymentDate.setCellValueFactory(data -> new SimpleStringProperty(FormatterUtil.formatDateSlash(data.getValue().getPaymentDate())));
        colAmount.setCellValueFactory(data -> new SimpleStringProperty(CURRENCY_FORMAT.format(data.getValue().getAmount()) + " đ"));
        
        colPaymentStatus.setCellValueFactory(data -> {
            boolean isCancelled = "CANCELLED".equalsIgnoreCase(data.getValue().getStatus());
            return new SimpleStringProperty(isCancelled ? "Đã hủy" : "Hợp lệ");
        });
        colPaymentStatus.setCellFactory(col -> new TableCell<Payment, String>() {
            private final Label badge = new Label();
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    badge.setText(item);
                    if ("Đã hủy".equalsIgnoreCase(item)) {
                        badge.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; -fx-padding: 3 8; -fx-background-radius: 4; -fx-font-weight: bold; -fx-font-size: 11px;");
                    } else {
                        badge.setStyle("-fx-background-color: #ECFDF5; -fx-text-fill: #047857; -fx-padding: 3 8; -fx-background-radius: 4; -fx-font-weight: bold; -fx-font-size: 11px;");
                    }
                    setGraphic(badge);
                    setText(null);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });

        colNote.setCellValueFactory(data -> {
            Payment p = data.getValue();
            if ("CANCELLED".equalsIgnoreCase(p.getStatus())) {
                String note = "[Lý do hủy: " + (p.getCancelReason() != null ? p.getCancelReason() : "Thao tác nhầm") + "]";
                if (p.getCancelledAt() != null) {
                    note += " (" + FormatterUtil.formatDateSlash(p.getCancelledAt()) + ")";
                }
                return new SimpleStringProperty(note);
            }
            return new SimpleStringProperty(p.getNote() != null ? p.getNote() : "");
        });

        // CellFactory cho colNote: Khi text dài có dấu ... thì hover chuột vào sẽ hiển thị Tooltip đầy đủ nội dung
        colNote.setCellFactory(col -> new TableCell<Payment, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.trim().isEmpty()) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    Tooltip tooltip = new Tooltip(item);
                    tooltip.setWrapText(true);
                    tooltip.setMaxWidth(400);
                    tooltip.setStyle("-fx-font-size: 12px;");
                    setTooltip(tooltip);
                }
            }
        });

        colAction.setCellFactory(col -> new TableCell<Payment, Void>() {
            private final Button btnCancel = new Button();
            private final org.kordamp.ikonli.javafx.FontIcon deleteIcon = new org.kordamp.ikonli.javafx.FontIcon("fth-trash-2");
            private final Tooltip cancelTooltip = new Tooltip("Hủy đợt thanh toán");
            {
                setAlignment(javafx.geometry.Pos.CENTER);
                btnCancel.setGraphic(deleteIcon);
                btnCancel.setTooltip(cancelTooltip);
                btnCancel.getStyleClass().addAll("btn-icon-action", "btn-icon-delete");
                btnCancel.setOnAction(e -> {
                    Payment payment = getTableView().getItems().get(getIndex());
                    handleCancelPayment(payment);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Payment payment = getTableView().getItems().get(getIndex());
                    if ("CANCELLED".equalsIgnoreCase(payment.getStatus())) {
                        setGraphic(null); // Không cho hủy đợt đã hủy
                    } else {
                        setGraphic(btnCancel);
                    }
                }
            }
        });

        paymentTable.setItems(paymentList);

        // Gắn formatter tự động phân tách hàng nghìn khi gõ
        FormatterUtil.attachCurrencyFormatter(txtPaymentAmount, null);
    }

    public void setInvoice(Invoice invoice) {
        this.currentInvoice = invoice;
        refreshInvoiceData();
    }

    public boolean isPaymentRecorded() {
        return paymentRecorded;
    }

    private void refreshInvoiceData() {
        if (currentInvoice == null) return;

        try {
            // Lấy lại dữ liệu mới nhất từ DB
            Invoice fresh = invoiceDao.findById(currentInvoice.getId());
            if (fresh != null) {
                this.currentInvoice = fresh;
            }
        } catch (SQLException e) {
            log.error("Không thể tải thông tin mới nhất của hóa đơn #{}", currentInvoice.getId(), e);
        }

        String customerDisplay = currentInvoice.getCustomerName() != null ? currentInvoice.getCustomerName() : ("ID #" + currentInvoice.getCustomerId());
        lblHeaderSubtitle.setText("Mã hóa đơn: " + currentInvoice.getCode() + " | Khách hàng: " + customerDisplay + " | Ngày lập: " + FormatterUtil.formatDateSlash(currentInvoice.getInvoiceDate()));

        lblTotalAmount.setText(CURRENCY_FORMAT.format(currentInvoice.getTotal()) + " đ");
        lblPaidAmount.setText(CURRENCY_FORMAT.format(currentInvoice.getPaid()) + " đ");
        lblDebtAmount.setText(CURRENCY_FORMAT.format(currentInvoice.getDebt()) + " đ");

        String status = FormatterUtil.formatInvoiceStatus(currentInvoice);
        lblInvoiceStatus.setText(status);
        if ("Đã thanh toán".equalsIgnoreCase(status) || "Đã hoàn thành".equalsIgnoreCase(status)) {
            lblInvoiceStatus.setStyle("-fx-background-color: #ECFDF5; -fx-text-fill: #047857; -fx-padding: 4 12; -fx-background-radius: 4; -fx-font-weight: bold;");
        } else if ("Còn nợ".equalsIgnoreCase(status)) {
            lblInvoiceStatus.setStyle("-fx-background-color: #FEF2F2; -fx-text-fill: #B91C1C; -fx-padding: 4 12; -fx-background-radius: 4; -fx-font-weight: bold;");
        } else {
            lblInvoiceStatus.setStyle("-fx-background-color: #F3F4F6; -fx-text-fill: #374151; -fx-padding: 4 12; -fx-background-radius: 4; -fx-font-weight: bold;");
        }

        boolean hasDebt = currentInvoice.getDebt() > 0;
        boxCollectDebt.setDisable(!hasDebt);
        btnConfirmPayment.setDisable(!hasDebt);
        if (btnQuickPayAll != null) {
            btnQuickPayAll.setDisable(!hasDebt);
        }

        if (hasDebt) {
            txtPaymentAmount.setText(FormatterUtil.formatNumberWithDots(currentInvoice.getDebt()));
        } else {
            txtPaymentAmount.clear();
            txtPaymentNote.clear();
        }

        loadPaymentHistory();
    }

    private void loadPaymentHistory() {
        if (currentInvoice == null) return;
        try {
            List<Payment> list = paymentDao.findByInvoiceId(currentInvoice.getId());
            paymentList.setAll(list);
            paymentTable.refresh();
        } catch (SQLException e) {
            log.error("Không thể tải lịch sử thanh toán của hóa đơn #{}", currentInvoice.getId(), e);
            showAlert(Alert.AlertType.ERROR, "Lỗi tải dữ liệu", "Không thể tải danh sách thanh toán: " + e.getMessage());
        }
    }

    @FXML
    private void handleQuickFillDebt() {
        if (currentInvoice != null && currentInvoice.getDebt() > 0) {
            txtPaymentAmount.setText(FormatterUtil.formatNumberWithDots(currentInvoice.getDebt()));
        }
    }

    @FXML
    private void handleConfirmPayment() {
        if (currentInvoice == null) return;

        if (currentInvoice.getDebt() <= 0) {
            showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Hóa đơn này đã hoàn tất thanh toán, không còn nợ.");
            return;
        }

        String amountText = txtPaymentAmount.getText();
        if (amountText == null || amountText.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Chưa nhập số tiền", "Vui lòng nhập số tiền khách thanh toán.");
            txtPaymentAmount.requestFocus();
            return;
        }

        long amount = FormatterUtil.parseNumberFromText(amountText);
        if (amount <= 0) {
            showAlert(Alert.AlertType.WARNING, "Số tiền không hợp lệ", "Số tiền thanh toán phải lớn hơn 0.");
            txtPaymentAmount.requestFocus();
            return;
        }

        if (amount > currentInvoice.getDebt()) {
            showAlert(Alert.AlertType.WARNING, "Số tiền vượt mức nợ",
                    "Số tiền thu (" + CURRENCY_FORMAT.format(amount) + " đ) vượt quá số nợ còn lại (" + CURRENCY_FORMAT.format(currentInvoice.getDebt()) + " đ).");
            txtPaymentAmount.requestFocus();
            return;
        }

        String note = txtPaymentNote.getText();
        if (note == null || note.trim().isEmpty()) {
            note = "Khách thanh toán nợ hóa đơn " + currentInvoice.getCode();
        }

        try {
            Invoice updated = paymentService.payInvoiceDebt(currentInvoice.getId(), amount, note);
            this.currentInvoice = updated;
            this.paymentRecorded = true;

            showAlert(Alert.AlertType.INFORMATION, "Thành công",
                    "Đã ghi nhận thanh toán " + CURRENCY_FORMAT.format(amount) + " đ thành công!");

            refreshInvoiceData();
        } catch (Exception e) {
            log.error("Lỗi khi ghi nhận thanh toán hóa đơn: {}", e.getMessage(), e);
            showAlert(Alert.AlertType.ERROR, "Lỗi thanh toán", "Không thể cập nhật trả nợ: " + e.getMessage());
        }
    }

    private void handleCancelPayment(Payment payment) {
        if (payment == null) return;

        TextInputDialog dialog = new TextInputDialog("Thao tác nhầm");
        dialog.setTitle("Xác nhận hủy đợt thu");
        dialog.setHeaderText("Hủy đợt thu: " + CURRENCY_FORMAT.format(payment.getAmount()) + " đ\n" +
                "Số tiền nợ của hóa đơn sẽ được hoàn trả lại tương ứng.");
        dialog.setContentText("Vui lòng nhập lý do hủy:");

        if (lblHeaderSubtitle != null && lblHeaderSubtitle.getScene() != null) {
            dialog.initOwner(lblHeaderSubtitle.getScene().getWindow());
        }

        dialog.showAndWait().ifPresent(reason -> {
            String actualReason = reason.trim();
            if (actualReason.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Chưa nhập lý do", "Vui lòng nhập lý do hủy đợt thu để lưu vết kiểm toán.");
                return;
            }

            try {
                Invoice updated = paymentService.cancelPayment(payment.getId(), actualReason);
                this.currentInvoice = updated;
                this.paymentRecorded = true;

                showAlert(Alert.AlertType.INFORMATION, "Thành công",
                        "Đã hủy đợt thu " + CURRENCY_FORMAT.format(payment.getAmount()) + " đ và lưu vết lý do thành công!");

                refreshInvoiceData();
            } catch (Exception e) {
                log.error("Lỗi khi hủy đợt thu: {}", e.getMessage(), e);
                showAlert(Alert.AlertType.ERROR, "Lỗi hủy thanh toán", "Không thể hủy đợt thu: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) lblHeaderSubtitle.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        if (lblHeaderSubtitle != null && lblHeaderSubtitle.getScene() != null) {
            alert.initOwner(lblHeaderSubtitle.getScene().getWindow());
        }
        alert.showAndWait();
    }
}
