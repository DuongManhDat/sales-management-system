package com.shop.controller;

import com.shop.model.Purchase;
import com.shop.service.PurchaseImportExportService;
import com.shop.service.PurchaseService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class PurchaseListController {

    private static final Logger log = LoggerFactory.getLogger(PurchaseListController.class);

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TableView<Purchase> purchaseTable;
    @FXML private TableColumn<Purchase, String> colCode;
    @FXML private TableColumn<Purchase, String> colDate;
    @FXML private TableColumn<Purchase, String> colSupplier;
    @FXML private TableColumn<Purchase, Long> colTotal;
    @FXML private TableColumn<Purchase, Long> colDebt;
    @FXML private TableColumn<Purchase, String> colStatus;
    @FXML private TableColumn<Purchase, Void> colAction;

    private final PurchaseService purchaseService = new PurchaseService();
    private final PurchaseImportExportService purchaseImportExportService = new PurchaseImportExportService();
    private final ObservableList<Purchase> masterData = FXCollections.observableArrayList();
    private final NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

    @FXML
    public void initialize() {
        statusFilter.getItems().addAll("Tất cả", "Đã thanh toán", "Còn nợ");
        statusFilter.getSelectionModel().selectFirst();

        setupTable();

        searchField.textProperty().addListener((observable, oldValue, newValue) -> loadData());
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> loadData());

        loadData();
    }

    private void setupTable() {
        colCode.setCellValueFactory(new PropertyValueFactory<>("code"));

        colDate.setCellValueFactory(new PropertyValueFactory<>("purchaseDate"));
        colDate.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.trim().isEmpty()) {
                    setText(null);
                } else {
                    setText(com.shop.util.FormatterUtil.formatDateToDdMmYyyy(item));
                }
            }
        });

        colSupplier.setCellValueFactory(new PropertyValueFactory<>("supplierName"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalCost"));
        colDebt.setCellValueFactory(new PropertyValueFactory<>("debt"));

        Callback<TableColumn<Purchase, Long>, TableCell<Purchase, Long>> currencyCellFactory = tc -> new TableCell<>() {
            @Override
            protected void updateItem(Long price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(currencyFormat.format(price) + " đ");
                }
            }
        };
        colTotal.setCellFactory(currencyCellFactory);
        colDebt.setCellFactory(currencyCellFactory);

        colStatus.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(com.shop.util.FormatterUtil.formatPurchaseStatus(data.getValue().getStatus())));
        colStatus.setCellFactory(column -> new TableCell<>() {
            private final Label badge = new Label();
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.trim().isEmpty()) {
                    setGraphic(null);
                    setText(null);
                } else {
                    badge.setText(item);
                    if ("Đã thanh toán".equalsIgnoreCase(item)) {
                        badge.setStyle("-fx-background-color: #ECFDF5; -fx-text-fill: #047857; -fx-padding: 3 10; -fx-background-radius: 4; -fx-font-weight: bold; -fx-font-size: 12px;");
                    } else {
                        badge.setStyle("-fx-background-color: #FEF2F2; -fx-text-fill: #B91C1C; -fx-padding: 3 10; -fx-background-radius: 4; -fx-font-weight: bold; -fx-font-size: 12px;");
                    }
                    setGraphic(badge);
                    setText(null);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });

        // Cột thao tác
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button viewBtn = new Button("Xem");
            {
                viewBtn.getStyleClass().add("btn-secondary");
                viewBtn.setOnAction(event -> {
                    Purchase purchase = getTableView().getItems().get(getIndex());
                    showDetailDialog(purchase);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(viewBtn);
                }
            }
        });
    }

    public void loadData() {
        String search = searchField.getText();
        String status = statusFilter.getValue();
        masterData.setAll(purchaseService.getAllPurchases(search, status));
        purchaseTable.setItems(masterData);
    }

    @FXML
    private void handleDownloadTemplate() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Lưu file Excel mẫu phiếu nhập hàng");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx"));
        fileChooser.setInitialFileName("mau-nhap-hang.xlsx");

        Stage stage = (Stage) purchaseTable.getScene().getWindow();
        File saveFile = fileChooser.showSaveDialog(stage);
        if (saveFile != null) {
            try {
                purchaseImportExportService.generateTemplate(saveFile);
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã tải file mẫu Excel nhập hàng thành công!");
            } catch (Exception e) {
                log.error("Lỗi khi tạo file mẫu Excel nhập hàng: {}", e.getMessage(), e);
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tạo file mẫu: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleImportPurchases() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn file Excel phiếu nhập hàng cần import");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx"));

        Stage stage = (Stage) purchaseTable.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try {
                PurchaseImportExportService.PurchaseImportResult result = purchaseImportExportService.validateImportFile(file);

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/purchase-import-dialog.fxml"));
                Parent root = loader.load();

                PurchaseImportDialogController dialogController = loader.getController();
                dialogController.setData(file, result, this::loadData);

                Stage dialogStage = new Stage();
                dialogStage.setTitle("Xem trước nhập file Excel phiếu nhập: " + file.getName());
                dialogStage.initModality(Modality.APPLICATION_MODAL);
                dialogStage.initOwner(stage);
                Scene scene = new Scene(root);
                if (getClass().getResource("/css/app.css") != null) {
                    scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
                }
                dialogStage.setScene(scene);
                dialogStage.showAndWait();

            } catch (Exception e) {
                log.error("Lỗi khi đọc file hoặc mở hộp thoại nhập Excel phiếu nhập: {}", e.getMessage(), e);
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể đọc file Excel: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleExportPurchases() {
        List<Purchase> items = purchaseTable.getItems();
        if (items == null || items.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Không có dữ liệu phiếu nhập để xuất Excel.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Xuất danh sách phiếu nhập ra Excel");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx"));
        fileChooser.setInitialFileName("danh-sach-phieu-nhap.xlsx");

        Stage stage = (Stage) purchaseTable.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            Task<Void> exportTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    purchaseImportExportService.exportPurchaseList(items, file);
                    return null;
                }
            };

            exportTask.setOnSucceeded(e -> Platform.runLater(() ->
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Xuất danh sách phiếu nhập ra Excel thành công!")
            ));

            exportTask.setOnFailed(e -> Platform.runLater(() -> {
                Throwable ex = exportTask.getException();
                log.error("Lỗi khi xuất danh sách phiếu nhập ra Excel: {}", ex.getMessage(), ex);
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể xuất file Excel: " + ex.getMessage());
            }));

            new Thread(exportTask).start();
        }
    }

    private void showDetailDialog(Purchase purchase) {
        if (purchase == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/purchase-detail-dialog.fxml"));
            Parent root = loader.load();

            PurchaseDetailController controller = loader.getController();
            controller.setPurchase(purchase);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Chi tiết Phiếu Nhập: " + purchase.getCode());
            Scene scene = new Scene(root, 880, 620);
            if (getClass().getResource("/css/app.css") != null) {
                scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            }
            stage.setScene(scene);
            stage.showAndWait();
        } catch (IOException e) {
            log.error("Lỗi khi mở chi tiết phiếu nhập: {}", e.getMessage(), e);
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
