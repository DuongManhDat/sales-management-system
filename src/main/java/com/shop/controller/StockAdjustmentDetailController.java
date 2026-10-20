package com.shop.controller;

import com.shop.model.StockAdjustment;
import com.shop.model.StockAdjustmentItem;
import com.shop.service.AdjustmentService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

public class StockAdjustmentDetailController {
    private static final Logger log = LoggerFactory.getLogger(StockAdjustmentDetailController.class);

    @FXML private Label lblCode;
    @FXML private Label lblDate;
    @FXML private Label lblStatus;
    @FXML private Label lblCanceledAt;
    @FXML private Label lblNote;
    @FXML private javafx.scene.control.Button btnCancelAdjustment;

    @FXML private TableView<StockAdjustmentItem> tableItems;
    @FXML private TableColumn<StockAdjustmentItem, String> colProductCode;
    @FXML private TableColumn<StockAdjustmentItem, String> colProductName;
    @FXML private TableColumn<StockAdjustmentItem, String> colCurrentQty;
    @FXML private TableColumn<StockAdjustmentItem, String> colActualQty;
    @FXML private TableColumn<StockAdjustmentItem, String> colVariance;
    @FXML private TableColumn<StockAdjustmentItem, String> colReason;

    private final AdjustmentService adjustmentService = new AdjustmentService();
    private final ObservableList<StockAdjustmentItem> itemList = FXCollections.observableArrayList();
    private StockAdjustment currentAdjustment;
    private boolean dataChanged = false;

    @FXML
    public void initialize() {
        setupTable();
    }

    private void setupTable() {
        colProductCode.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getProductCode()));
        colProductName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getProductName()));
        colCurrentQty.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("%.2f", cellData.getValue().getCurrentQty())));
        colActualQty.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("%.2f", cellData.getValue().getActualQty())));
        colVariance.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("%.2f", cellData.getValue().getVariance())));
        colReason.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getReason()));

        tableItems.setItems(itemList);
    }

    public void setAdjustment(StockAdjustment adjustment) {
        if (adjustment == null) return;
        this.currentAdjustment = adjustment;
        lblCode.setText(adjustment.getCode());
        lblDate.setText(com.shop.util.FormatterUtil.formatDateToDdMmYyyy(adjustment.getCreatedAt()));
        lblNote.setText(adjustment.getNote() != null ? adjustment.getNote() : "");

        boolean isCanceled = "CANCELED".equalsIgnoreCase(adjustment.getStatus()) || "Đã hủy".equalsIgnoreCase(adjustment.getStatus());
        if (isCanceled) {
            lblStatus.setText("Đã hủy");
            lblStatus.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: #b91c1c; -fx-padding: 4 12; -fx-background-radius: 4; -fx-font-weight: bold;");
            String cancelTime = adjustment.getCanceledAt() != null && !adjustment.getCanceledAt().trim().isEmpty()
                    ? com.shop.util.FormatterUtil.formatDateToDdMmYyyy(adjustment.getCanceledAt()) : "Không xác định";
            lblCanceledAt.setText("(Hủy lúc: " + cancelTime + ")");
            lblCanceledAt.setVisible(true);
            lblCanceledAt.setManaged(true);

            if (btnCancelAdjustment != null) {
                btnCancelAdjustment.setVisible(false);
                btnCancelAdjustment.setManaged(false);
            }
        } else {
            lblStatus.setText("Hoàn tất");
            lblStatus.setStyle("-fx-background-color: #ecfdf5; -fx-text-fill: #047857; -fx-padding: 4 12; -fx-background-radius: 4; -fx-font-weight: bold;");
            lblCanceledAt.setVisible(false);
            lblCanceledAt.setManaged(false);

            if (btnCancelAdjustment != null) {
                btnCancelAdjustment.setVisible(true);
                btnCancelAdjustment.setManaged(true);
            }
        }

        loadItems(adjustment.getId());
    }

    private void loadItems(int adjustmentId) {
        try {
            List<StockAdjustmentItem> items = adjustmentService.getAdjustmentItems(adjustmentId);
            itemList.setAll(items);
        } catch (SQLException e) {
            log.error("Failed to load adjustment items", e);
        }
    }

    @FXML
    private void handleCancelAdjustment() {
        if (currentAdjustment == null) return;

        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION,
                "Bạn có chắc chắn muốn hủy phiếu kiểm kho " + currentAdjustment.getCode() + "?\nSố lượng tồn kho và các lô dôi dư tương ứng sẽ được hoàn nguyên.",
                javafx.scene.control.ButtonType.YES,
                javafx.scene.control.ButtonType.NO
        );
        alert.setTitle("Xác nhận hủy phiếu kiểm");
        alert.setHeaderText("Xác nhận thao tác hủy");

        alert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.YES) {
                try {
                    adjustmentService.cancelAdjustment(currentAdjustment.getId());
                    this.dataChanged = true;

                    javafx.scene.control.Alert info = new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.INFORMATION,
                            "Phiếu kiểm kho " + currentAdjustment.getCode() + " đã được hủy thành công."
                    );
                    info.setTitle("Thông báo");
                    info.setHeaderText(null);
                    info.showAndWait();

                    StockAdjustment refreshed = adjustmentService.getAdjustmentById(currentAdjustment.getId());
                    setAdjustment(refreshed);
                } catch (Exception e) {
                    log.error("Lỗi khi hủy phiếu kiểm kho", e);
                    javafx.scene.control.Alert err = new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.ERROR,
                            "Không thể hủy phiếu kiểm kho: " + e.getMessage()
                    );
                    err.setTitle("Lỗi");
                    err.setHeaderText(null);
                    err.showAndWait();
                }
            }
        });
    }

    public boolean isDataChanged() {
        return dataChanged;
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) tableItems.getScene().getWindow();
        stage.close();
    }
}
