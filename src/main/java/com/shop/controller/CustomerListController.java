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

    @FXML
    private void handleImport() {
        System.out.println("Import clicked");
    }

    @FXML
    private void handleExport() {
        System.out.println("Export clicked");
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
