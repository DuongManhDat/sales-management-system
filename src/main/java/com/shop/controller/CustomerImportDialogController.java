package com.shop.controller;

import com.shop.model.CustomerImportItem;
import com.shop.service.CustomerImportExportService;
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
import java.time.format.DateTimeFormatter;

public class CustomerImportDialogController {

    private static final Logger log = LoggerFactory.getLogger(CustomerImportDialogController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private VBox bannerBox;
    @FXML private Label lblBannerTitle;
    @FXML private Label lblBannerDesc;

    @FXML private TableView<CustomerImportItem> tableView;
    @FXML private TableColumn<CustomerImportItem, Integer> colRow;
    @FXML private TableColumn<CustomerImportItem, String> colCode;
    @FXML private TableColumn<CustomerImportItem, String> colName;
    @FXML private TableColumn<CustomerImportItem, String> colPhone;
    @FXML private TableColumn<CustomerImportItem, String> colEmail;
    @FXML private TableColumn<CustomerImportItem, String> colDob;
    @FXML private TableColumn<CustomerImportItem, String> colGender;
    @FXML private TableColumn<CustomerImportItem, String> colAddress;
    @FXML private TableColumn<CustomerImportItem, String> colNote;
    @FXML private TableColumn<CustomerImportItem, String> colError;

    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label lblProgress;
    @FXML private Button btnConfirm;

    private final CustomerImportExportService importExportService = new CustomerImportExportService();

    private File currentFile;
    private CustomerImportExportService.CustomerImportResult currentResult;
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
                    CustomerImportItem rowItem = getRowItem(this);
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
                    CustomerImportItem rowItem = getRowItem(this);
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
                    CustomerImportItem rowItem = getRowItem(this);
                    if (rowItem != null && rowItem.isNameError()) {
                        setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        colPhone.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPhone()));
        colPhone.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    CustomerImportItem rowItem = getRowItem(this);
                    if (rowItem != null && rowItem.isPhoneError()) {
                        setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        colEmail.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
        colEmail.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    CustomerImportItem rowItem = getRowItem(this);
                    if (rowItem != null && rowItem.isEmailError()) {
                        setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        colDob.setCellValueFactory(data -> {
            CustomerImportItem item = data.getValue();
            if (item.isDobError()) {
                return new SimpleStringProperty(item.getDateOfBirthStr());
            }
            if (item.getDateOfBirth() != null) {
                return new SimpleStringProperty(item.getDateOfBirth().format(DATE_FORMATTER));
            }
            return new SimpleStringProperty("");
        });
        colDob.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    CustomerImportItem rowItem = getRowItem(this);
                    if (rowItem != null && rowItem.isDobError()) {
                        setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        colGender.setCellValueFactory(data -> {
            CustomerImportItem item = data.getValue();
            if (item.isGenderError()) {
                return new SimpleStringProperty(item.getGenderStr());
            }
            if (item.getGender() != null) {
                return new SimpleStringProperty(item.getGender().toString());
            }
            return new SimpleStringProperty("");
        });
        colGender.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    CustomerImportItem rowItem = getRowItem(this);
                    if (rowItem != null && rowItem.isGenderError()) {
                        setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        colAddress.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAddress()));
        colNote.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNote()));

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

    private CustomerImportItem getRowItem(TableCell<?, ?> cell) {
        if (cell.getTableRow() != null && cell.getTableRow().getItem() instanceof CustomerImportItem item) {
            return item;
        }
        int index = cell.getIndex();
        if (index >= 0 && index < tableView.getItems().size()) {
            return tableView.getItems().get(index);
        }
        return null;
    }

    public void setData(File file, CustomerImportExportService.CustomerImportResult result, Runnable onImportSuccess) {
        this.currentFile = file;
        this.currentResult = result;
        this.onImportSuccessCallback = onImportSuccess;

        if (!result.isAllValid()) {
            // Chỉ hiển thị (preview) những dòng bị lỗi
            java.util.List<CustomerImportItem> errorRows = result.getAllRows().stream()
                    .filter(CustomerImportItem::hasError)
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
        fileChooser.setTitle("Lưu file Excel mẫu nhập khách hàng");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx"));
        fileChooser.setInitialFileName("mau-nhap-khach-hang.xlsx");

        Stage stage = (Stage) tableView.getScene().getWindow();
        File saveFile = fileChooser.showSaveDialog(stage);
        if (saveFile != null) {
            try {
                importExportService.generateTemplate(saveFile);
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã tải file mẫu Excel thành công!");
            } catch (Exception e) {
                log.error("Lỗi khi tạo file mẫu Excel khách hàng: {}", e.getMessage(), e);
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
        lblProgress.setText("Đang nhập dữ liệu khách hàng vào hệ thống...");

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
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Import thành công " + currentResult.getValidRows() + " khách hàng!");
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
                log.error("Lỗi khi thực hiện import khách hàng vào CSDL: {}", ex.getMessage(), ex);
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
