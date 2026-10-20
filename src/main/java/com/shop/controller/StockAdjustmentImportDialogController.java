package com.shop.controller;

import com.shop.model.StockAdjustmentImportItem;
import com.shop.service.StockAdjustmentImportExportService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

public class StockAdjustmentImportDialogController {
    private static final Logger log = LoggerFactory.getLogger(StockAdjustmentImportDialogController.class);

    @FXML private VBox bannerBox;
    @FXML private Label lblBannerTitle;
    @FXML private Label lblBannerDesc;

    @FXML private TableView<StockAdjustmentImportItem> tableView;
    @FXML private TableColumn<StockAdjustmentImportItem, Integer> colRow;
    @FXML private TableColumn<StockAdjustmentImportItem, String> colCode;
    @FXML private TableColumn<StockAdjustmentImportItem, String> colName;
    @FXML private TableColumn<StockAdjustmentImportItem, String> colCurrentQty;
    @FXML private TableColumn<StockAdjustmentImportItem, String> colActualQty;
    @FXML private TableColumn<StockAdjustmentImportItem, String> colVariance;
    @FXML private TableColumn<StockAdjustmentImportItem, String> colReason;
    @FXML private TableColumn<StockAdjustmentImportItem, String> colError;

    @FXML private Button btnConfirm;

    private final StockAdjustmentImportExportService importExportService = new StockAdjustmentImportExportService();
    private StockAdjustmentImportExportService.ImportResult currentResult;
    private Consumer<List<StockAdjustmentImportItem>> onConfirmCallback;

    @FXML
    public void initialize() {
        setupTableColumns();
    }

    private void setupTableColumns() {
        colRow.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getRowNumber()).asObject());
        colRow.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.valueOf(item));
                    StockAdjustmentImportItem rowItem = getRowItem(this);
                    if (rowItem != null && rowItem.hasError()) {
                        setStyle("-fx-background-color: #FEF2F2; -fx-text-fill: #991B1B; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    } else {
                        setStyle("-fx-alignment: CENTER;");
                    }
                }
            }
        });

        colCode.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProductCode()));
        colCode.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    StockAdjustmentImportItem rowItem = getRowItem(this);
                    if (rowItem != null && (rowItem.isProductCodeError() || rowItem.isDuplicateError() || rowItem.isCrossAdjustmentError())) {
                        setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-font-weight: bold; -fx-alignment: CENTER_LEFT;");
                    } else {
                        setStyle("-fx-alignment: CENTER_LEFT;");
                    }
                }
            }
        });

        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProductName()));
        colName.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER_LEFT;");
                }
            }
        });

        colCurrentQty.setCellValueFactory(data -> {
            Double val = data.getValue().getCurrentQty();
            return new SimpleStringProperty(val != null ? String.format("%.2f", val) : "—");
        });
        colCurrentQty.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER_RIGHT;");
                }
            }
        });

        colActualQty.setCellValueFactory(data -> {
            Double val = data.getValue().getActualQty();
            return new SimpleStringProperty(val != null ? String.format("%.2f", val) : "—");
        });
        colActualQty.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    StockAdjustmentImportItem rowItem = getRowItem(this);
                    if (rowItem != null && (rowItem.isActualQtyError() || rowItem.isDuplicateError() || rowItem.isCrossAdjustmentError())) {
                        setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-font-weight: bold; -fx-alignment: CENTER_RIGHT;");
                    } else {
                        setStyle("-fx-alignment: CENTER_RIGHT; -fx-font-weight: bold;");
                    }
                }
            }
        });

        colVariance.setCellValueFactory(data -> {
            Double val = data.getValue().getVariance();
            if (val == null) return new SimpleStringProperty("—");
            String prefix = val > 0 ? "+" : "";
            return new SimpleStringProperty(prefix + String.format("%.2f", val));
        });
        colVariance.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.startsWith("+")) {
                        setStyle("-fx-alignment: CENTER_RIGHT; -fx-text-fill: #047857; -fx-font-weight: bold;");
                    } else if (item.startsWith("-")) {
                        setStyle("-fx-alignment: CENTER_RIGHT; -fx-text-fill: #B91C1C; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-alignment: CENTER_RIGHT;");
                    }
                }
            }
        });

        colReason.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReason() != null ? data.getValue().getReason() : ""));
        colReason.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER_LEFT;");
                }
            }
        });

        colError.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getErrorMessage()));
        colError.setCellFactory(column -> new TableCell<>() {
            private final Label label = new Label();
            {
                label.setWrapText(true);
                label.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold; -fx-line-spacing: 3px;");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.trim().isEmpty()) {
                    setGraphic(null);
                    setText(null);
                    setTooltip(null);
                    setStyle("");
                } else {
                    String displayText = item.contains("; ") ? "• " + item.replace("; ", "\n• ") : item;
                    label.setText(displayText);
                    label.maxWidthProperty().bind(column.widthProperty().subtract(20));
                    setGraphic(label);
                    setText(null);
                    Tooltip tooltip = new Tooltip(displayText);
                    tooltip.setStyle("-fx-font-size: 13px;");
                    setTooltip(tooltip);
                    setStyle("-fx-alignment: CENTER_LEFT; -fx-padding: 8 10;");
                }
            }
        });
    }

    private StockAdjustmentImportItem getRowItem(TableCell<?, ?> cell) {
        int index = cell.getIndex();
        if (index >= 0 && index < tableView.getItems().size()) {
            return tableView.getItems().get(index);
        }
        return null;
    }

    public void setData(StockAdjustmentImportExportService.ImportResult result, Consumer<List<StockAdjustmentImportItem>> onConfirm) {
        this.currentResult = result;
        this.onConfirmCallback = onConfirm;

        if (!result.isAllValid()) {
            // Hiển thị những dòng bị lỗi để người dùng rà soát
            List<StockAdjustmentImportItem> errorRows = result.getAllRows().stream()
                    .filter(StockAdjustmentImportItem::hasError)
                    .toList();
            tableView.getItems().setAll(errorRows);

            btnConfirm.setDisable(true);
            bannerBox.setStyle("-fx-background-color: #FEF2F2; -fx-border-color: #EF4444; -fx-padding: 14; -fx-background-radius: 4; -fx-border-radius: 4; -fx-border-width: 1;");
            lblBannerTitle.setText("⚠️ PHÁT HIỆN " + result.getErrorRows() + " DÒNG DỮ LIỆU BỊ LỖI (TỔNG SỐ " + result.getTotalRows() + " DÒNG)");
            lblBannerTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #991B1B;");
            lblBannerDesc.setText("Toàn bộ quá trình Import bị tạm khóa theo quy tắc toàn vẹn dữ liệu. Bảng dưới đây CHỈ HIỂN THỊ " + result.getErrorRows() + " DÒNG BỊ LỖI để bạn dễ dàng rà soát. Vui lòng kiểm tra các ô được bôi đỏ, sửa lại file Excel và thử lại.");
            lblBannerDesc.setStyle("-fx-font-size: 13px; -fx-text-fill: #B91C1C;");
        } else {
            // Khi toàn bộ dữ liệu hợp lệ thì preview toàn bộ các dòng
            tableView.getItems().setAll(result.getAllRows());

            btnConfirm.setDisable(false);
            bannerBox.setStyle("-fx-background-color: #ECFDF5; -fx-border-color: #10B981; -fx-padding: 14; -fx-background-radius: 4; -fx-border-radius: 4; -fx-border-width: 1;");
            lblBannerTitle.setText("✅ TẤT CẢ " + result.getValidRows() + " DÒNG ĐỀU HỢP LỆ 100%");
            lblBannerTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #065F46;");
            lblBannerDesc.setText("Dữ liệu kiểm kho đã sẵn sàng. Nhấn [Xác nhận tạo phiếu kiểm kho] để tạo phiếu kiểm kho và cập nhật tồn kho vào hệ thống.");
            lblBannerDesc.setStyle("-fx-font-size: 13px; -fx-text-fill: #047857;");
        }
    }

    @FXML
    private void handleDownloadTemplate() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Lưu file Excel mẫu kiểm kho");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files (*.xlsx)", "*.xlsx"));
        fileChooser.setInitialFileName("mau_kiem_kho.xlsx");

        Stage stage = (Stage) tableView.getScene().getWindow();
        File saveFile = fileChooser.showSaveDialog(stage);

        if (saveFile != null) {
            try {
                importExportService.generateTemplate(saveFile);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Thành công");
                alert.setHeaderText(null);
                alert.setContentText("Đã tải file mẫu kiểm kho thành công tại:\n" + saveFile.getAbsolutePath());
                alert.showAndWait();
            } catch (IOException | SQLException e) {
                log.error("Lỗi khi tạo file mẫu Excel kiểm kho", e);
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Lỗi");
                alert.setHeaderText("Không thể tạo file mẫu");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            }
        }
    }

    @FXML
    private void handleConfirmImport() {
        if (currentResult == null || !currentResult.isAllValid()) {
            return;
        }

        if (onConfirmCallback != null) {
            onConfirmCallback.accept(currentResult.getAllRows());
        }

        handleCancel();
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) tableView.getScene().getWindow();
        stage.close();
    }
}
