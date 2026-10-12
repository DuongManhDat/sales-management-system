package com.shop.controller;

import com.shop.dao.InvoiceDao;
import com.shop.dao.ReturnInvoiceDao;
import com.shop.dao.ReturnInvoiceItemDao;
import com.shop.model.Invoice;
import com.shop.model.ReturnInvoice;
import com.shop.model.ReturnInvoiceItem;
import com.shop.service.SalesService;
import com.shop.util.FormatterUtil;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.List;

public class ReturnDetailController {
    private static final Logger log = LoggerFactory.getLogger(ReturnDetailController.class);
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,###");

    @FXML private Label lblCode;
    @FXML private Label lblDate;
    @FXML private Label lblInvoiceCode;
    @FXML private Label lblCustomer;
    @FXML private Label lblStatus;
    @FXML private Label lblCanceledAt;

    @FXML private TableView<ReturnInvoiceItem> tableItems;
    @FXML private TableColumn<ReturnInvoiceItem, Number> colIndex;
    @FXML private TableColumn<ReturnInvoiceItem, String> colProductCode;
    @FXML private TableColumn<ReturnInvoiceItem, String> colProductName;
    @FXML private TableColumn<ReturnInvoiceItem, String> colUnitName;
    @FXML private TableColumn<ReturnInvoiceItem, Integer> colReturnQty;
    @FXML private TableColumn<ReturnInvoiceItem, Long> colRefundPrice;
    @FXML private TableColumn<ReturnInvoiceItem, Long> colAmount;

    @FXML private Label lblGrossRefund;
    @FXML private Label lblReturnFee;
    @FXML private Label lblNetRefund;

    @FXML private Button btnCancelReturn;
    @FXML private Button btnRecreate;

    private final ReturnInvoiceDao returnInvoiceDao = new ReturnInvoiceDao();
    private final ReturnInvoiceItemDao returnInvoiceItemDao = new ReturnInvoiceItemDao();
    private final InvoiceDao invoiceDao = new InvoiceDao();
    private final SalesService salesService = new SalesService();

    private ReturnInvoice currentReturnInvoice;
    private List<ReturnInvoiceItem> currentItems;
    private boolean dataChanged = false;

    @FXML
    public void initialize() {
        setupTable();
    }

    private void setupTable() {
        colIndex.setCellValueFactory(column -> new ReadOnlyObjectWrapper<>(tableItems.getItems().indexOf(column.getValue()) + 1));
        colProductCode.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getProductCode() != null ? data.getValue().getProductCode() : "SP#" + data.getValue().getProductId()
        ));
        colProductName.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getProductName() != null ? data.getValue().getProductName() : "—"
        ));
        colUnitName.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getUnitName() != null ? data.getValue().getUnitName() : "—"
        ));
        colReturnQty.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("returnQty"));

        colRefundPrice.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("refundPrice"));
        colRefundPrice.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Long price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(CURRENCY_FORMAT.format(price) + " đ");
                }
                setStyle("-fx-alignment: CENTER_RIGHT;");
            }
        });

        colAmount.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("amount"));
        colAmount.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Long amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText(null);
                } else {
                    setText(CURRENCY_FORMAT.format(amount) + " đ");
                }
                setStyle("-fx-alignment: CENTER_RIGHT;");
            }
        });
    }

    public void setReturnInvoice(ReturnInvoice returnInvoice) {
        if (returnInvoice == null) return;
        this.currentReturnInvoice = returnInvoice;

        lblCode.setText(returnInvoice.getCode() != null ? returnInvoice.getCode() : "—");
        lblDate.setText(FormatterUtil.formatDateToDdMmYyyy(returnInvoice.getReturnDate()));

        String invCode = returnInvoice.getInvoiceCode();
        if (invCode == null || invCode.trim().isEmpty()) {
            invCode = "HĐ #" + returnInvoice.getInvoiceId();
        }
        lblInvoiceCode.setText(invCode);

        String custName = returnInvoice.getCustomerName();
        if (custName == null || custName.trim().isEmpty()) {
            custName = "KH #" + returnInvoice.getCustomerId();
        }
        lblCustomer.setText(custName);

        String status = returnInvoice.getStatus();
        boolean isCanceled = "CANCELED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status) || "Đã hủy".equalsIgnoreCase(status);

        if (isCanceled) {
            lblStatus.setText("Đã hủy");
            lblStatus.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: #b91c1c; -fx-padding: 4 12; -fx-background-radius: 4; -fx-font-weight: bold;");
            String cancelTime = returnInvoice.getCanceledAt() != null && !returnInvoice.getCanceledAt().trim().isEmpty()
                    ? FormatterUtil.formatDateToDdMmYyyy(returnInvoice.getCanceledAt()) : "Không xác định";
            lblCanceledAt.setText("Thời gian hủy: " + cancelTime);
            lblCanceledAt.setVisible(true);
            lblCanceledAt.setManaged(true);

            if (btnCancelReturn != null) {
                btnCancelReturn.setVisible(false);
                btnCancelReturn.setManaged(false);
            }
            if (btnRecreate != null) {
                btnRecreate.setVisible(true);
                btnRecreate.setManaged(true);
            }
        } else {
            lblStatus.setText("Hoàn tất");
            lblStatus.setStyle("-fx-background-color: #ecfdf5; -fx-text-fill: #047857; -fx-padding: 4 12; -fx-background-radius: 4; -fx-font-weight: bold;");
            lblCanceledAt.setVisible(false);
            lblCanceledAt.setManaged(false);

            if (btnCancelReturn != null) {
                btnCancelReturn.setVisible(true);
                btnCancelReturn.setManaged(true);
            }
            if (btnRecreate != null) {
                btnRecreate.setVisible(false);
                btnRecreate.setManaged(false);
            }
        }

        long gross = returnInvoice.getTotalRefund();
        long fee = returnInvoice.getReturnFee();
        long net = Math.max(0, gross - fee);

        lblGrossRefund.setText(CURRENCY_FORMAT.format(gross) + " đ");
        lblReturnFee.setText(CURRENCY_FORMAT.format(fee) + " đ");
        lblNetRefund.setText(CURRENCY_FORMAT.format(net) + " đ");

        try {
            this.currentItems = returnInvoiceItemDao.findByReturnInvoiceId(returnInvoice.getId());
            tableItems.setItems(FXCollections.observableArrayList(currentItems));
        } catch (SQLException e) {
            log.error("Failed to load return invoice items", e);
        }
    }

    @FXML
    private void handleCancelReturn() {
        if (currentReturnInvoice == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận hủy phiếu trả hàng");
        confirm.setHeaderText("Hủy phiếu trả hàng: " + currentReturnInvoice.getCode() + "?");
        confirm.setContentText("Khi hủy, toàn bộ số lượng hàng đã trả sẽ bị thu hồi khỏi tồn kho và công nợ hóa đơn gốc sẽ được phục hồi. Bạn có chắc chắn muốn hủy phiếu này?");

        Stage stage = (Stage) lblCode.getScene().getWindow();
        confirm.initOwner(stage);

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    salesService.cancelReturnInvoice(currentReturnInvoice.getId());
                    this.dataChanged = true;
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Phiếu trả hàng " + currentReturnInvoice.getCode() + " đã được chuyển sang trạng thái Đã hủy.");
                    ReturnInvoice refreshed = returnInvoiceDao.findById(currentReturnInvoice.getId());
                    setReturnInvoice(refreshed);
                } catch (Exception e) {
                    log.error("Lỗi khi hủy phiếu trả hàng", e);
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể hủy phiếu trả hàng: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleRecreate() {
        if (currentReturnInvoice == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/return-form.fxml"));
            Parent root = loader.load();

            ReturnFormController formController = loader.getController();

            Invoice origInvoice = invoiceDao.findById(currentReturnInvoice.getInvoiceId());
            if (origInvoice != null) {
                formController.setInitialReturnData(origInvoice, currentItems, currentReturnInvoice.getReturnFee());
            }

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            if (lblCode != null && lblCode.getScene() != null) {
                stage.initOwner(lblCode.getScene().getWindow());
            }
            stage.setTitle("Tạo Phiếu Trả Hàng Từ Phiếu Cũ (" + currentReturnInvoice.getCode() + ")");

            javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
            double popupWidth = screenBounds.getWidth() * 0.8;
            double popupHeight = Math.min(screenBounds.getHeight() * 0.85, 750);

            Scene scene = new Scene(root, popupWidth, popupHeight);
            if (getClass().getResource("/css/app.css") != null) {
                scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            }
            stage.setScene(scene);
            stage.showAndWait();

            this.dataChanged = true;
            handleClose();
        } catch (IOException | SQLException e) {
            log.error("Lỗi khi tạo từ phiếu cũ", e);
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể mở form tạo từ phiếu cũ: " + e.getMessage());
        }
    }

    @FXML
    public void handleClose() {
        Stage stage = null;
        if (lblCode != null && lblCode.getScene() != null) {
            stage = (Stage) lblCode.getScene().getWindow();
        }
        if (stage != null) {
            stage.close();
        }
    }

    public boolean isDataChanged() {
        return dataChanged;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
