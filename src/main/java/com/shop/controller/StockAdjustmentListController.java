package com.shop.controller;

import com.shop.model.StockAdjustment;
import com.shop.service.AdjustmentService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class StockAdjustmentListController {
    private static final Logger log = LoggerFactory.getLogger(StockAdjustmentListController.class);

    @FXML private TextField searchCodeField;
    @FXML private DatePicker dateFilter;
    @FXML private TableView<StockAdjustment> tableAdjustments;
    @FXML private TableColumn<StockAdjustment, String> colCode;
    @FXML private TableColumn<StockAdjustment, String> colDate;
    @FXML private TableColumn<StockAdjustment, String> colStatus;
    @FXML private TableColumn<StockAdjustment, String> colNote;

    private final AdjustmentService adjustmentService = new AdjustmentService();
    private final ObservableList<StockAdjustment> adjustmentList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupDatePickerConverter();
        setupTable();
        setupListeners();
        loadData();
    }

    private void setupDatePickerConverter() {
        dateFilter.setConverter(new StringConverter<LocalDate>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            @Override
            public String toString(LocalDate date) {
                return date != null ? formatter.format(date) : "";
            }
            @Override
            public LocalDate fromString(String string) {
                if (string == null || string.trim().isEmpty()) return null;
                try {
                    return LocalDate.parse(string.trim(), formatter);
                } catch (Exception e) {
                    return null;
                }
            }
        });
    }

    private void setupListeners() {
        searchCodeField.textProperty().addListener((obs, oldVal, newVal) -> loadData());
        dateFilter.valueProperty().addListener((obs, oldVal, newVal) -> loadData());
    }

    private void setupTable() {
        colCode.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCode()));
        colDate.setCellValueFactory(cellData -> new SimpleStringProperty(com.shop.util.FormatterUtil.formatDateToDdMmYyyy(cellData.getValue().getCreatedAt())));
        colStatus.setCellValueFactory(cellData -> {
            String status = cellData.getValue().getStatus();
            if ("CANCELED".equalsIgnoreCase(status) || "Đã hủy".equalsIgnoreCase(status)) {
                return new SimpleStringProperty("Đã hủy");
            }
            return new SimpleStringProperty("Hoàn tất");
        });
        colStatus.setCellFactory(com.shop.util.StatusBadge.createCellFactory());
        colNote.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNote()));

        tableAdjustments.setItems(adjustmentList);

        tableAdjustments.setRowFactory(tv -> {
            TableRow<StockAdjustment> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    StockAdjustment rowData = row.getItem();
                    showDetailDialog(rowData);
                }
            });
            return row;
        });
    }

    private void loadData() {
        try {
            String code = searchCodeField != null ? searchCodeField.getText() : null;
            String dateStr = null;
            if (dateFilter != null && dateFilter.getValue() != null) {
                dateStr = dateFilter.getValue().toString(); // YYYY-MM-DD
            }
            List<StockAdjustment> list = adjustmentService.filterAdjustments(code, dateStr);
            adjustmentList.setAll(list);
        } catch (SQLException e) {
            log.error("Error loading stock adjustments", e);
        }
    }

    @FXML
    private void handleClearDate() {
        if (dateFilter != null) {
            dateFilter.setValue(null);
        }
    }

    @FXML
    private void handleDownloadTemplate() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Lưu file Excel mẫu kiểm kho");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files (*.xlsx)", "*.xlsx"));
        fileChooser.setInitialFileName("mau_kiem_kho.xlsx");

        Stage stage = (Stage) tableAdjustments.getScene().getWindow();
        File saveFile = fileChooser.showSaveDialog(stage);

        if (saveFile != null) {
            try {
                com.shop.service.StockAdjustmentImportExportService importService = new com.shop.service.StockAdjustmentImportExportService();
                importService.generateTemplate(saveFile);
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã tải file mẫu kiểm kho thành công tại:\n" + saveFile.getAbsolutePath());
            } catch (IOException | SQLException e) {
                log.error("Lỗi khi tạo file mẫu Excel kiểm kho", e);
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tạo file mẫu: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleImportExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn file Excel kiểm kho để nhập");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files (*.xlsx, *.xls)", "*.xlsx", "*.xls")
        );

        Stage stage = (Stage) tableAdjustments.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            try {
                com.shop.service.StockAdjustmentImportExportService importService = new com.shop.service.StockAdjustmentImportExportService();
                com.shop.service.StockAdjustmentImportExportService.ImportResult result = importService.validateImportFile(file);

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/stock-adjustment-import-dialog.fxml"));
                Parent root = loader.load();

                StockAdjustmentImportDialogController controller = loader.getController();
                controller.setData(result, validRows -> {
                    try {
                        // Tạo phiếu kiểm kho trực tiếp từ file Excel hợp lệ
                        java.util.Map<Integer, com.shop.model.StockAdjustmentImportItem> dedupMap = new java.util.LinkedHashMap<>();
                        for (com.shop.model.StockAdjustmentImportItem item : validRows) {
                            if (item.getProductId() != null) {
                                dedupMap.put(item.getProductId(), item);
                            }
                        }

                        StockAdjustment adj = new StockAdjustment();
                        adj.setNote("Import từ file Excel: " + file.getName());

                        List<com.shop.model.StockAdjustmentItem> items = new java.util.ArrayList<>();
                        for (com.shop.model.StockAdjustmentImportItem importItem : dedupMap.values()) {
                            com.shop.model.StockAdjustmentItem item = new com.shop.model.StockAdjustmentItem();
                            item.setProductId(importItem.getProductId());
                            item.setProductCode(importItem.getProductCode());
                            item.setProductName(importItem.getProductName());
                            item.setCurrentQty(importItem.getCurrentQty() != null ? importItem.getCurrentQty() : 0.0);
                            item.setActualQty(importItem.getActualQty() != null ? importItem.getActualQty() : 0.0);
                            item.setVariance(item.getActualQty() - item.getCurrentQty());
                            item.setReason(importItem.getReason() != null ? importItem.getReason() : "");
                            item.setCostPrice(importItem.getCostPrice() != null ? importItem.getCostPrice() : 0L);
                            items.add(item);
                        }

                        adjustmentService.createAdjustment(adj, items);
                        loadData();
                        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã tạo phiếu kiểm kho thành công với " + items.size() + " mặt hàng.");
                    } catch (Exception e) {
                        log.error("Lỗi khi lưu phiếu kiểm kho từ file Excel", e);
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tạo phiếu kiểm kho: " + e.getMessage());
                    }
                });

                Stage dialogStage = new Stage();
                dialogStage.setTitle("Xem trước dữ liệu Nhập Excel Kiểm kho: " + file.getName());
                dialogStage.initModality(Modality.APPLICATION_MODAL);
                dialogStage.initOwner(stage);
                Scene scene = new Scene(root);
                if (getClass().getResource("/css/app.css") != null) {
                    scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
                }
                dialogStage.setScene(scene);
                dialogStage.showAndWait();

            } catch (Exception e) {
                log.error("Lỗi khi đọc file Excel kiểm kho", e);
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể xử lý file Excel: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleCreateAdjustment() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/stock-adjustment-view.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Kiểm Kho");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
            loadData();
        } catch (IOException e) {
            log.error("Failed to open stock adjustment view", e);
        }
    }

    private void showDetailDialog(StockAdjustment adjustment) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/stock-adjustment-detail-dialog.fxml"));
            Parent root = loader.load();
            
            StockAdjustmentDetailController controller = loader.getController();
            controller.setAdjustment(adjustment);
            
            Stage stage = new Stage();
            stage.setTitle("Chi tiết Phiếu Kiểm Kho: " + adjustment.getCode());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            if (controller.isDataChanged()) {
                loadData();
            }
        } catch (IOException e) {
            log.error("Failed to open stock adjustment detail view", e);
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
