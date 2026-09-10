package com.shop.controller;

import com.shop.model.PurchaseImportItem;
import com.shop.service.PurchaseImportExportService;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class PurchaseImportDialogController {

    private static final Logger log = LoggerFactory.getLogger(PurchaseImportDialogController.class);

    @FXML private VBox bannerBox;
    @FXML private Label lblBannerTitle;
    @FXML private Label lblBannerDesc;

    @FXML private TableView<PurchaseImportItem> tableView;
    @FXML private TableColumn<PurchaseImportItem, Integer> colRow;
    @FXML private TableColumn<PurchaseImportItem, String> colPurchaseCode;
    @FXML private TableColumn<PurchaseImportItem, String> colSupplierCode;
    @FXML private TableColumn<PurchaseImportItem, String> colPurchaseDate;
    @FXML private TableColumn<PurchaseImportItem, String> colPaidAmount;
    @FXML private TableColumn<PurchaseImportItem, String> colProductCode;
    @FXML private TableColumn<PurchaseImportItem, String> colProductName;
    @FXML private TableColumn<PurchaseImportItem, String> colUnit;
    @FXML private TableColumn<PurchaseImportItem, String> colQuantity;
    @FXML private TableColumn<PurchaseImportItem, String> colUnitPrice;
    @FXML private TableColumn<PurchaseImportItem, String> colLineTotal;
    @FXML private TableColumn<PurchaseImportItem, String> colError;

    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label lblProgress;
    @FXML private Button btnConfirm;

    private final PurchaseImportExportService importExportService = new PurchaseImportExportService();
    private final NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

    private File currentFile;
    private PurchaseImportExportService.PurchaseImportResult currentResult;
    private Runnable onImportSuccessCallback;

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
                    PurchaseImportItem rowItem = getRowItem(this);
                    if (rowItem != null && rowItem.hasError()) {
                        setStyle("-fx-background-color: #FEF2F2; -fx-text-fill: #991B1B; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    } else {
                        setStyle("-fx-alignment: CENTER;");
                    }
                }
            }
        });

        colPurchaseCode.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPurchaseCode() != null ? data.getValue().getPurchaseCode() : ""));
        colPurchaseCode.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    PurchaseImportItem rowItem = getRowItem(this);
                    if (rowItem != null && rowItem.isPurchaseCodeError()) {
                        setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-font-weight: bold; -fx-alignment: CENTER_LEFT;");
                    } else {
                        setStyle("-fx-alignment: CENTER_LEFT;");
                    }
                }
            }
        });

        colSupplierCode.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSupplierCode() != null ? data.getValue().getSupplierCode() : ""));
        colSupplierCode.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    PurchaseImportItem rowItem = getRowItem(this);
                    if (rowItem != null && rowItem.isSupplierCodeError()) {
                        setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-font-weight: bold; -fx-alignment: CENTER_LEFT;");
                    } else {
                        setStyle("-fx-alignment: CENTER_LEFT;");
                    }
                }
            }
        });

        colPurchaseDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPurchaseDate() != null ? com.shop.util.FormatterUtil.formatDateToDdMmYyyy(data.getValue().getPurchaseDate()) : ""));
        colPurchaseDate.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    PurchaseImportItem rowItem = getRowItem(this);
                    if (rowItem != null && rowItem.isPurchaseDateError()) {
                        setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    } else {
                        setStyle("-fx-alignment: CENTER;");
                    }
                }
            }
        });

        colPaidAmount.setCellValueFactory(data -> {
            Long paid = data.getValue().getPaidAmount();
            return new SimpleStringProperty(paid != null ? currencyFormat.format(paid) + " đ" : "0 đ");
        });
        colPaidAmount.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    PurchaseImportItem rowItem = getRowItem(this);
                    if (rowItem != null && rowItem.isPaidAmountError()) {
                        setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-font-weight: bold; -fx-alignment: CENTER_RIGHT;");
                    } else {
                        setStyle("-fx-alignment: CENTER_RIGHT;");
                    }
                }
            }
        });

        colProductCode.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProductCode() != null ? data.getValue().getProductCode() : ""));
        colProductCode.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    PurchaseImportItem rowItem = getRowItem(this);
                    if (rowItem != null && rowItem.isProductCodeError()) {
                        setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-font-weight: bold; -fx-alignment: CENTER_LEFT;");
                    } else {
                        setStyle("-fx-alignment: CENTER_LEFT;");
                    }
                }
            }
        });

        colProductName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProductName() != null ? data.getValue().getProductName() : ""));
        
        colUnit.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUnitName() != null ? data.getValue().getUnitName() : ""));
        colUnit.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });

        colQuantity.setCellValueFactory(data -> {
            Integer qty = data.getValue().getQty();
            return new SimpleStringProperty(qty != null ? String.valueOf(qty) : "0");
        });
        colQuantity.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    PurchaseImportItem rowItem = getRowItem(this);
                    if (rowItem != null && rowItem.isQuantityError()) {
                        setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    } else {
                        setStyle("-fx-alignment: CENTER;");
                    }
                }
            }
        });

        colUnitPrice.setCellValueFactory(data -> {
            Long price = data.getValue().getCostPrice();
            return new SimpleStringProperty(price != null ? currencyFormat.format(price) + " đ" : "0 đ");
        });
        colUnitPrice.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    PurchaseImportItem rowItem = getRowItem(this);
                    if (rowItem != null && rowItem.isUnitPriceError()) {
                        setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-font-weight: bold; -fx-alignment: CENTER_RIGHT;");
                    } else {
                        setStyle("-fx-alignment: CENTER_RIGHT;");
                    }
                }
            }
        });

        colLineTotal.setCellValueFactory(data -> {
            Long total = data.getValue().getLineTotal();
            return new SimpleStringProperty(total != null ? currencyFormat.format(total) + " đ" : "0 đ");
        });
        colLineTotal.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER_RIGHT; -fx-font-weight: bold;");
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

    private PurchaseImportItem getRowItem(TableCell<?, ?> cell) {
        if (cell.getTableRow() != null && cell.getTableRow().getItem() instanceof PurchaseImportItem item) {
            return item;
        }
        int index = cell.getIndex();
        if (index >= 0 && index < tableView.getItems().size()) {
            return tableView.getItems().get(index);
        }
        return null;
    }

    public void setData(File file, PurchaseImportExportService.PurchaseImportResult result, Runnable onImportSuccess) {
        this.currentFile = file;
        this.currentResult = result;
        this.onImportSuccessCallback = onImportSuccess;

        if (!result.isAllValid()) {
            // Hiển thị những dòng bị lỗi để người dùng rà soát
            List<PurchaseImportItem> errorRows = result.getAllRows().stream()
                    .filter(PurchaseImportItem::hasError)
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
            lblBannerDesc.setText("Dữ liệu phiếu nhập đã sẵn sàng để nạp vào hệ thống. Nhấn [Xác nhận Import] để tiến hành cập nhật kho và lưu trữ.");
            lblBannerDesc.setStyle("-fx-font-size: 13px; -fx-text-fill: #047857;");
        }
    }

    @FXML
    private void handleDownloadTemplate() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Lưu file Excel mẫu phiếu nhập hàng");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx"));
        fileChooser.setInitialFileName("mau-nhap-hang.xlsx");

        Stage stage = (Stage) tableView.getScene().getWindow();
        File saveFile = fileChooser.showSaveDialog(stage);
        if (saveFile != null) {
            try {
                importExportService.generateTemplate(saveFile);
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã tải file mẫu Excel thành công!");
            } catch (Exception e) {
                log.error("Lỗi khi tạo file mẫu Excel nhập hàng: {}", e.getMessage(), e);
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tạo file mẫu: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleConfirmImport() {
        if (currentResult == null || !currentResult.isAllValid()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Không thể import vì file có chứa dữ liệu lỗi.");
            return;
        }

        btnConfirm.setDisable(true);
        progressIndicator.setVisible(true);
        progressIndicator.setManaged(true);
        lblProgress.setText("Đang nhập dữ liệu và cập nhật tồn kho...");

        Task<Void> importTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                importExportService.executeImport(currentResult.getAllRows());
                return null;
            }
        };

        importTask.setOnSucceeded(event -> {
            Platform.runLater(() -> {
                progressIndicator.setVisible(false);
                progressIndicator.setManaged(false);
                lblProgress.setText("");
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Import thành công " + currentResult.getValidRows() + " dòng chi tiết hàng hóa vào hệ thống!");
                if (onImportSuccessCallback != null) {
                    onImportSuccessCallback.run();
                }
                closeDialog();
            });
        });

        importTask.setOnFailed(event -> {
            Platform.runLater(() -> {
                progressIndicator.setVisible(false);
                progressIndicator.setManaged(false);
                lblProgress.setText("");
                btnConfirm.setDisable(false);
                Throwable ex = importTask.getException();
                log.error("Lỗi khi thực hiện import phiếu nhập vào CSDL: {}", ex.getMessage(), ex);
                showAlert(Alert.AlertType.ERROR, "Lỗi Import", "Đã xảy ra lỗi khi lưu vào cơ sở dữ liệu:\n" + ex.getMessage());
            });
        });

        new Thread(importTask).start();
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) tableView.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
