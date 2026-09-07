package com.shop.controller;

import com.shop.model.Customer;
import com.shop.service.CustomerService;
import com.shop.viewmodel.CustomerListViewModel;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import java.util.List;

public class CustomerListController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private TableView<Customer> customerTable;
    @FXML private TableColumn<Customer, String> colCode;
    @FXML private TableColumn<Customer, String> colName;
    @FXML private TableColumn<Customer, String> colPhone;
    @FXML private TableColumn<Customer, String> colEmail;
    @FXML private TableColumn<Customer, Void> colDebt; // Assuming we'll format this later or bind differently
    @FXML private TableColumn<Customer, Boolean> colStatus;
    @FXML private TableColumn<Customer, Void> colAction;

    private CustomerListViewModel viewModel;
    private CustomerService customerService;
    private PauseTransition debouncePause;

    @FXML
    public void initialize() {
        customerService = new CustomerService();
        viewModel = new CustomerListViewModel();

        setupTable();
        setupBindings();
        loadData();
    }

    private void setupTable() {
        colCode.setCellValueFactory(new PropertyValueFactory<>("code"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        
        colStatus.setCellValueFactory(new PropertyValueFactory<>("active"));
        colStatus.setCellFactory(column -> new TableCell<Customer, Boolean>() {
            @Override
            protected void updateItem(Boolean isActive, boolean empty) {
                super.updateItem(isActive, empty);
                if (empty || isActive == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label label = new Label(isActive ? "Hoạt động" : "Ngừng HĐ");
                    label.setStyle("-fx-background-color: " + (isActive ? "#DCFCE7" : "#FEE2E2") + 
                                   "; -fx-text-fill: " + (isActive ? "#166534" : "#991B1B") + 
                                   "; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-weight: bold;");
                    setGraphic(label);
                }
            }
        });

        colAction.setCellFactory(param -> new TableCell<Customer, Void>() {
            private final Button btnDetail = new Button("Chi tiết");
            private final Button btnEdit = new Button("Sửa");
            private final HBox pane = new HBox(8, btnDetail, btnEdit);

            {
                btnDetail.getStyleClass().add("btn-secondary");
                btnEdit.getStyleClass().add("btn-secondary");

                btnDetail.setOnAction(event -> {
                    Customer customer = getTableView().getItems().get(getIndex());
                    handleDetail(customer);
                });

                btnEdit.setOnAction(event -> {
                    Customer customer = getTableView().getItems().get(getIndex());
                    handleEdit(customer);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        customerTable.setItems(viewModel.getFilteredCustomers());
    }

    private void setupBindings() {
        viewModel.filterStatusProperty().bind(statusFilterCombo.valueProperty());
        
        debouncePause = new PauseTransition(Duration.millis(200));
        debouncePause.setOnFinished(event -> viewModel.searchKeywordProperty().set(searchField.getText()));
        
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            debouncePause.playFromStart();
        });
    }

    private void loadData() {
        Task<List<Customer>> loadTask = customerService.getAllCustomersTask();
        loadTask.setOnSucceeded(e -> viewModel.setCustomers(loadTask.getValue()));
        loadTask.setOnFailed(e -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Lỗi tải dữ liệu khách hàng.");
            alert.show();
        });
        new Thread(loadTask).start();
    }

    @FXML
    private void handleAdd() {
        openFormDialog(null);
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CustomerListController.class);
    private final com.shop.service.CustomerImportExportService customerImportExportService = new com.shop.service.CustomerImportExportService();

    @FXML
    private void handleDownloadTemplate() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Lưu file Excel mẫu nhập khách hàng");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx"));
        fileChooser.setInitialFileName("mau-nhap-khach-hang.xlsx");

        javafx.stage.Stage stage = (javafx.stage.Stage) customerTable.getScene().getWindow();
        java.io.File saveFile = fileChooser.showSaveDialog(stage);
        if (saveFile != null) {
            try {
                customerImportExportService.generateTemplate(saveFile);
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã tải file mẫu Excel thành công!");
            } catch (Exception e) {
                log.error("Lỗi khi tạo file mẫu Excel khách hàng: {}", e.getMessage(), e);
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tạo file mẫu: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleImport() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Chọn file Excel danh sách khách hàng cần nhập");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx"));

        javafx.stage.Stage stage = (javafx.stage.Stage) customerTable.getScene().getWindow();
        java.io.File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try {
                com.shop.service.CustomerImportExportService.CustomerImportResult result = customerImportExportService.validateImport(file);

                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/customer-import-dialog.fxml"));
                javafx.scene.Parent root = loader.load();

                CustomerImportDialogController dialogController = loader.getController();
                dialogController.setData(file, result, this::loadData);

                javafx.stage.Stage dialogStage = new javafx.stage.Stage();
                dialogStage.setTitle("Xem trước nhập file Excel: " + file.getName());
                dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                dialogStage.initOwner(stage);
                dialogStage.setScene(new javafx.scene.Scene(root));
                dialogStage.showAndWait();

            } catch (Exception e) {
                log.error("Lỗi khi đọc file hoặc mở hộp thoại nhập Excel khách hàng: {}", e.getMessage(), e);
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể đọc file Excel: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleExport() {
        List<Customer> items = customerTable.getItems();
        if (items == null || items.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Không có dữ liệu khách hàng để xuất Excel.");
            return;
        }

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Xuất danh sách khách hàng ra Excel");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx"));
        fileChooser.setInitialFileName("danh-sach-khach-hang.xlsx");

        javafx.stage.Stage stage = (javafx.stage.Stage) customerTable.getScene().getWindow();
        java.io.File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            Task<Void> exportTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    customerImportExportService.exportCustomers(file, items);
                    return null;
                }
            };

            exportTask.setOnSucceeded(e -> javafx.application.Platform.runLater(() ->
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Xuất file Excel thành công!")
            ));

            exportTask.setOnFailed(e -> javafx.application.Platform.runLater(() -> {
                Throwable ex = exportTask.getException();
                log.error("Lỗi khi xuất danh sách khách hàng ra Excel: {}", ex.getMessage(), ex);
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể xuất file Excel: " + ex.getMessage());
            }));

            new Thread(exportTask).start();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void handleDetail(Customer customer) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/customer-detail.fxml"));
            javafx.scene.Parent view = loader.load();
            
            CustomerDetailController controller = loader.getController();
            
            // The root of customer-list is a VBox. Its parent is the BorderPane (mainPane).
            javafx.scene.layout.BorderPane mainPane = (javafx.scene.layout.BorderPane) searchField.getScene().getRoot();
            javafx.scene.Node previousView = mainPane.getCenter();
            
            controller.initData(customer, () -> {
                // handleBack
                mainPane.setCenter(previousView);
                loadData(); // reload just in case
            }, () -> {
                // handleEdit
                handleEdit(customer);
                controller.initData(customer, () -> {
                    mainPane.setCenter(previousView);
                    loadData();
                }, () -> handleEdit(customer)); // re-init to refresh data after edit
            });
            
            mainPane.setCenter(view);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private void handleEdit(Customer customer) {
        openFormDialog(customer);
    }

    private void openFormDialog(Customer customer) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/customer-form-dialog.fxml"));
            javafx.scene.Parent root = loader.load();
            
            CustomerFormController controller = loader.getController();
            controller.initData(customer, customerService, this::loadData);
            
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setTitle(customer == null ? "Thêm khách hàng" : "Sửa khách hàng");
            stage.setScene(new javafx.scene.Scene(root));
            stage.showAndWait();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
