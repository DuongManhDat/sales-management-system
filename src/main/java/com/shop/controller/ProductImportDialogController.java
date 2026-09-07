package com.shop.controller;

import com.shop.model.ProductImportItem;
import com.shop.service.ImportExportService;
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
import java.util.Locale;

public class ProductImportDialogController {

    private static final Logger log = LoggerFactory.getLogger(ProductImportDialogController.class);

    @FXML private VBox bannerBox;
    @FXML private Label lblBannerTitle;
    @FXML private Label lblBannerDesc;

    @FXML private TableView<ProductImportItem> tableView;
    @FXML private TableColumn<ProductImportItem, Integer> colRow;
    @FXML private TableColumn<ProductImportItem, String> colCode;
    @FXML private TableColumn<ProductImportItem, String> colName;
    @FXML private TableColumn<ProductImportItem, String> colUnit;
    @FXML private TableColumn<ProductImportItem, String> colCategory;
    @FXML private TableColumn<ProductImportItem, String> colCostPrice;
    @FXML private TableColumn<ProductImportItem, String> colPrice;
    @FXML private TableColumn<ProductImportItem, String> colInitialStock;
    @FXML private TableColumn<ProductImportItem, String> colNote;
    @FXML private TableColumn<ProductImportItem, String> colError;

    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label lblProgress;
    @FXML private Button btnConfirm;

    private final ImportExportService importExportService = new ImportExportService();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    private File currentFile;
    private ImportExportService.ImportResult currentResult;
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
                    ProductImportItem rowItem = getRowItem(this);
                    if (rowItem != null && rowItem.hasError()) {
                        setStyle("-fx-background-color: #FEF2F2; -fx-text-fill: #991B1B; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    } else {
                        setStyle("-fx-alignment: CENTER;");
                    }
                }
            }
        });

        colCode.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCode()));
        colCode.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    ProductImportItem rowItem = getRowItem(this);
                    if (rowItem != null && rowItem.isCodeError()) {
                        setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colName.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    ProductImportItem rowItem = getRowItem(this);
                    if (rowItem != null && rowItem.isNameError()) {
                        setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        colUnit.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUnitName()));
        colUnit.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    ProductImportItem rowItem = getRowItem(this);
                    if (rowItem != null && rowItem.isUnitError()) {
                        setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        colCategory.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategoryName()));
        colCategory.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    ProductImportItem rowItem = getRowItem(this);
                    if (rowItem != null && rowItem.isCategoryError()) {
                        setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        colCostPrice.setCellValueFactory(data -> {
            ProductImportItem item = data.getValue();
            if (item.isCostPriceError()) {
                return new SimpleStringProperty(item.getCostPriceStr());
            }
            return new SimpleStringProperty(currencyFormat.format(item.getCostPrice()));
        });
        colCostPrice.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    ProductImportItem rowItem = getRowItem(this);
                    if (rowItem != null && rowItem.isCostPriceError()) {
                        setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-font-weight: bold; -fx-alignment: CENTER_RIGHT;");
                    } else {
                        setStyle("-fx-alignment: CENTER_RIGHT;");
                    }
                }
            }
        });

        colPrice.setCellValueFactory(data -> {
            ProductImportItem item = data.getValue();
            if (item.isPriceError()) {
                return new SimpleStringProperty(item.getSalePriceStr());
            }
            return new SimpleStringProperty(currencyFormat.format(item.getSalePrice()));
        });
        colPrice.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    ProductImportItem rowItem = getRowItem(this);
                    if (rowItem != null && rowItem.isPriceError()) {
                        setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-font-weight: bold; -fx-alignment: CENTER_RIGHT;");
                    } else {
                        setStyle("-fx-alignment: CENTER_RIGHT;");
                    }
                }
            }
        });

        colInitialStock.setCellValueFactory(data -> {
            ProductImportItem item = data.getValue();
            if (item.isStockError()) {
                return new SimpleStringProperty(item.getInitialStockStr());
            }
            return new SimpleStringProperty(String.valueOf(item.getInitialStock()));
        });
        colInitialStock.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    ProductImportItem rowItem = getRowItem(this);
                    if (rowItem != null && rowItem.isStockError()) {
                        setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-font-weight: bold; -fx-alignment: CENTER_RIGHT;");
                    } else {
                        setStyle("-fx-alignment: CENTER_RIGHT;");
                    }
                }
            }
        });

        colNote.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNote()));

        colError.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getErrorMessage()));
        colError.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.trim().isEmpty()) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
                }
            }
        });
    }

    private ProductImportItem getRowItem(TableCell<?, ?> cell) {
        if (cell.getTableRow() != null && cell.getTableRow().getItem() instanceof ProductImportItem item) {
            return item;
        }
        int index = cell.getIndex();
        if (index >= 0 && index < tableView.getItems().size()) {
            return tableView.getItems().get(index);
        }
        return null;
    }

    public void setData(File file, ImportExportService.ImportResult result, Runnable onImportSuccess) {
        this.currentFile = file;
        this.currentResult = result;
        this.onImportSuccessCallback = onImportSuccess;

        if (!result.isAllValid()) {
            // Chỉ hiển thị (preview) những dòng bị lỗi theo yêu cầu người dùng
            java.util.List<ProductImportItem> errorRows = result.getAllRows().stream()
                    .filter(ProductImportItem::hasError)
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
            lblBannerDesc.setText("Dữ liệu đã sẵn sàng để nhập vào hệ thống. Nhấn [Xác nhận Import] để tiến hành lưu trữ.");
            lblBannerDesc.setStyle("-fx-font-size: 13px; -fx-text-fill: #047857;");
        }
    }

    @FXML
    private void handleDownloadTemplate() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Lưu file Excel mẫu nhập hàng hóa");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx"));
        fileChooser.setInitialFileName("mau-nhap-hang-hoa.xlsx");

        Stage stage = (Stage) tableView.getScene().getWindow();
        File saveFile = fileChooser.showSaveDialog(stage);
        if (saveFile != null) {
            try {
                importExportService.generateImportTemplate(saveFile);
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã tải file mẫu Excel thành công!");
            } catch (Exception e) {
                log.error("Lỗi khi tạo file mẫu Excel: {}", e.getMessage(), e);
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
        lblProgress.setText("Đang nhập dữ liệu vào hệ thống...");

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
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Import thành công!");
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
                log.error("Lỗi khi thực hiện import vào CSDL: {}", ex.getMessage(), ex);
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
