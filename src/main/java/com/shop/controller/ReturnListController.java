package com.shop.controller;

import com.shop.dao.ReturnInvoiceDao;
import com.shop.model.ReturnInvoice;
import com.shop.util.FormatterUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReturnListController {
    private static final Logger log = LoggerFactory.getLogger(ReturnListController.class);

    @FXML private TextField txtFilterReturnCode;
    @FXML private TextField txtFilterInvoiceCode;
    @FXML private TextField txtFilterCustomer;
    @FXML private DatePicker dpFilterReturnDate;
    @FXML private ComboBox<String> cbFilterStatus;

    @FXML private TableView<ReturnInvoice> returnTable;
    @FXML private TableColumn<ReturnInvoice, String> colCode;
    @FXML private TableColumn<ReturnInvoice, String> colInvoiceCode;
    @FXML private TableColumn<ReturnInvoice, String> colCustomer;
    @FXML private TableColumn<ReturnInvoice, String> colDate;
    @FXML private TableColumn<ReturnInvoice, String> colStatus;

    private final ReturnInvoiceDao returnInvoiceDao = new ReturnInvoiceDao();

    @FXML
    public void initialize() {
        cbFilterStatus.setItems(FXCollections.observableArrayList("Tất cả", "Hoàn tất", "Đã hủy"));
        cbFilterStatus.getSelectionModel().selectFirst();

        setupDatePickerConverter();

        // Cột: Mã Phiếu, Hóa đơn gốc, Khách hàng, Ngày trả và Trạng thái
        colCode.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCode()));
        colInvoiceCode.setCellValueFactory(data -> {
            String invCode = data.getValue().getInvoiceCode();
            if (invCode != null && !invCode.trim().isEmpty()) {
                return new SimpleStringProperty(invCode);
            }
            return new SimpleStringProperty("HĐ #" + data.getValue().getInvoiceId());
        });
        colCustomer.setCellValueFactory(data -> {
            String custName = data.getValue().getCustomerName();
            if (custName != null && !custName.trim().isEmpty()) {
                return new SimpleStringProperty(custName);
            }
            return new SimpleStringProperty("KH #" + data.getValue().getCustomerId());
        });
        colDate.setCellValueFactory(data -> new SimpleStringProperty(
                FormatterUtil.formatDateToDdMmYyyy(data.getValue().getReturnDate())
        ));
        colStatus.setCellValueFactory(data -> {
            String status = data.getValue().getStatus();
            if ("CANCELED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status) || "Đã hủy".equalsIgnoreCase(status)) {
                return new SimpleStringProperty("Đã hủy");
            }
            return new SimpleStringProperty("Hoàn tất");
        });
        colStatus.setCellFactory(com.shop.util.StatusBadge.createCellFactory());

        // Double click vào hàng để mở chi tiết
        returnTable.setRowFactory(tv -> {
            TableRow<ReturnInvoice> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    openDetailDialog(row.getItem());
                }
            });
            return row;
        });

        // Lắng nghe sự kiện filter
        txtFilterReturnCode.textProperty().addListener((obs, old, val) -> loadReturns());
        txtFilterInvoiceCode.textProperty().addListener((obs, old, val) -> loadReturns());
        txtFilterCustomer.textProperty().addListener((obs, old, val) -> loadReturns());
        dpFilterReturnDate.valueProperty().addListener((obs, old, val) -> loadReturns());
        cbFilterStatus.valueProperty().addListener((obs, old, val) -> loadReturns());

        loadReturns();
    }

    private void setupDatePickerConverter() {
        dpFilterReturnDate.setConverter(new StringConverter<LocalDate>() {
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

    @FXML
    private void handleClearDate() {
        dpFilterReturnDate.setValue(null);
    }

    @FXML
    private void loadReturns() {
        try {
            String returnCode = txtFilterReturnCode.getText();
            String invoiceCode = txtFilterInvoiceCode.getText();
            String customer = txtFilterCustomer.getText();
            String dateParam = null;
            if (dpFilterReturnDate.getValue() != null) {
                dateParam = dpFilterReturnDate.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE);
            }
            String status = cbFilterStatus.getValue();

            List<ReturnInvoice> returns = returnInvoiceDao.searchReturnInvoices(returnCode, invoiceCode, customer, dateParam, status);
            returnTable.setItems(FXCollections.observableArrayList(returns));
        } catch (SQLException e) {
            log.error("Failed to load returns", e);
        }
    }

    @FXML
    private void handleViewDetail() {
        ReturnInvoice selected = returnTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Chưa chọn phiếu");
            alert.setHeaderText(null);
            alert.setContentText("Vui lòng chọn 1 phiếu trả hàng từ bảng để xem chi tiết.");
            alert.showAndWait();
            return;
        }
        openDetailDialog(selected);
    }

    private void openDetailDialog(ReturnInvoice returnInvoice) {
        if (returnInvoice == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/return-detail-dialog.fxml"));
            Parent root = loader.load();

            ReturnDetailController controller = loader.getController();
            controller.setReturnInvoice(returnInvoice);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            if (returnTable != null && returnTable.getScene() != null) {
                stage.initOwner(returnTable.getScene().getWindow());
            }
            stage.setTitle("Chi tiết Phiếu Trả Hàng - " + returnInvoice.getCode());

            javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
            double popupWidth = screenBounds.getWidth() * 0.8;
            double popupHeight = Math.min(screenBounds.getHeight() * 0.85, 750);

            Scene scene = new Scene(root, popupWidth, popupHeight);
            if (getClass().getResource("/css/app.css") != null) {
                scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            }
            stage.setScene(scene);
            stage.showAndWait();

            // Nếu có thay đổi trạng thái (hủy phiếu), tải lại danh sách
            if (controller.isDataChanged()) {
                loadReturns();
            }
        } catch (IOException e) {
            log.error("Lỗi khi mở dialog chi tiết phiếu trả hàng: {}", e.getMessage(), e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setHeaderText(null);
            alert.setContentText("Không thể mở chi tiết phiếu trả hàng: " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void openReturnForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/return-form.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            if (returnTable != null && returnTable.getScene() != null) {
                stage.initOwner(returnTable.getScene().getWindow());
            }
            stage.setTitle("Tạo Phiếu Trả Hàng Mới");

            javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
            double popupWidth = screenBounds.getWidth() * 0.8;
            double popupHeight = Math.min(screenBounds.getHeight() * 0.85, 750);

            Scene scene = new Scene(root, popupWidth, popupHeight);
            if (getClass().getResource("/css/app.css") != null) {
                scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            }
            stage.setScene(scene);
            stage.showAndWait();

            // Tự động làm mới danh sách sau khi tạo phiếu
            loadReturns();
        } catch (IOException e) {
            log.error("Lỗi khi mở popup tạo phiếu trả hàng: {}", e.getMessage(), e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setHeaderText(null);
            alert.setContentText("Không thể mở form tạo phiếu trả hàng: " + e.getMessage());
            alert.showAndWait();
        }
    }
}
