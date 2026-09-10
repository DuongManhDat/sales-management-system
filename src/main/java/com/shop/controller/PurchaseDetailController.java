package com.shop.controller;

import com.shop.model.Purchase;
import com.shop.model.PurchaseItem;
import com.shop.service.PurchaseImportExportService;
import com.shop.service.PurchaseService;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class PurchaseDetailController {

    private static final Logger log = LoggerFactory.getLogger(PurchaseDetailController.class);

    @FXML private Label lblCode;
    @FXML private Label lblDate;
    @FXML private Label lblSupplier;
    @FXML private Label lblStatus;
    @FXML private Label lblNote;

    @FXML private TableView<PurchaseItem> tableItems;
    @FXML private TableColumn<PurchaseItem, Number> colIndex;
    @FXML private TableColumn<PurchaseItem, String> colProductCode;
    @FXML private TableColumn<PurchaseItem, String> colProductName;
    @FXML private TableColumn<PurchaseItem, String> colUnitName;
    @FXML private TableColumn<PurchaseItem, Integer> colQty;
    @FXML private TableColumn<PurchaseItem, Long> colPrice;
    @FXML private TableColumn<PurchaseItem, Long> colAmount;

    @FXML private Label lblTotal;
    @FXML private Label lblPaid;
    @FXML private Label lblDebt;

    private final PurchaseService purchaseService = new PurchaseService();
    private final PurchaseImportExportService purchaseImportExportService = new PurchaseImportExportService();
    private final NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

    private Purchase currentPurchase;

    @FXML
    public void initialize() {
        setupTable();
    }

    private void setupTable() {
        colIndex.setCellValueFactory(column -> new ReadOnlyObjectWrapper<>(tableItems.getItems().indexOf(column.getValue()) + 1));
        colProductCode.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProductCode() != null ? data.getValue().getProductCode() : ""));
        colProductName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProductName() != null ? data.getValue().getProductName() : ""));
        colUnitName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUnitName() != null ? data.getValue().getUnitName() : ""));

        colQty.setCellValueFactory(new PropertyValueFactory<>("qty"));

        colPrice.setCellValueFactory(new PropertyValueFactory<>("costPrice"));
        colPrice.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Long price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(currencyFormat.format(price) + " đ");
                }
            }
        });

        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colAmount.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Long amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText(null);
                } else {
                    setText(currencyFormat.format(amount) + " đ");
                }
            }
        });
    }

    public void setPurchase(Purchase purchase) {
        if (purchase == null) return;
        this.currentPurchase = purchase;

        lblCode.setText(purchase.getCode() != null ? purchase.getCode() : "-");
        lblDate.setText(com.shop.util.FormatterUtil.formatDateToDdMmYyyy(purchase.getPurchaseDate()));
        lblSupplier.setText(purchase.getSupplierName() != null ? purchase.getSupplierName() : "Không xác định");
        lblNote.setText(purchase.getNote() != null && !purchase.getNote().trim().isEmpty() ? purchase.getNote() : "Không có ghi chú");

        String status = com.shop.util.FormatterUtil.formatPurchaseStatus(purchase.getStatus());
        lblStatus.setText(status);
        if ("Đã thanh toán".equalsIgnoreCase(status)) {
            lblStatus.setStyle("-fx-background-color: #ecfdf5; -fx-text-fill: #047857; -fx-padding: 4 12; -fx-background-radius: 4; -fx-font-weight: bold;");
        } else {
            lblStatus.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: #b91c1c; -fx-padding: 4 12; -fx-background-radius: 4; -fx-font-weight: bold;");
        }

        lblTotal.setText(currencyFormat.format(purchase.getTotalCost()) + " đ");
        lblPaid.setText(currencyFormat.format(purchase.getPaid()) + " đ");
        lblDebt.setText(currencyFormat.format(purchase.getDebt()) + " đ");

        List<PurchaseItem> items = purchaseService.getItemsByPurchaseId(purchase.getId());
        tableItems.setItems(FXCollections.observableArrayList(items));
    }

    @FXML
    private void handleExportDetail() {
        if (currentPurchase == null) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Không có thông tin phiếu nhập để xuất file.");
            return;
        }

        List<PurchaseItem> items = tableItems.getItems();
        if (items == null || items.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Phiếu nhập không có mặt hàng nào để xuất.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Xuất chi tiết phiếu nhập ra Excel");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx"));
        String codeName = currentPurchase.getCode() != null ? currentPurchase.getCode() : "phieu-nhap";
        fileChooser.setInitialFileName("chi-tiet-phieu-nhap-" + codeName + ".xlsx");

        Stage stage = (Stage) lblCode.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            Task<Void> exportTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    purchaseImportExportService.exportPurchaseDetail(currentPurchase, items, file);
                    return null;
                }
            };

            exportTask.setOnSucceeded(e -> Platform.runLater(() ->
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Xuất chi tiết phiếu nhập ra Excel thành công!")
            ));

            exportTask.setOnFailed(e -> Platform.runLater(() -> {
                Throwable ex = exportTask.getException();
                log.error("Lỗi khi xuất chi tiết phiếu nhập ra Excel: {}", ex.getMessage(), ex);
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể xuất file Excel: " + ex.getMessage());
            }));

            new Thread(exportTask).start();
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

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
