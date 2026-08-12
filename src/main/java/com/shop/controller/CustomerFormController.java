package com.shop.controller;

import com.shop.model.Customer;
import com.shop.model.Gender;
import com.shop.service.CustomerService;
import com.shop.viewmodel.CustomerFormViewModel;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class CustomerFormController {

    @FXML private Label lblTitle;
    @FXML private TextField txtName;
    @FXML private TextField txtPhone;
    @FXML private Label lblPhoneError;
    @FXML private TextField txtEmail;
    @FXML private DatePicker dpDateOfBirth;
    @FXML private ComboBox<Gender> comboGender;
    @FXML private TextField txtAddress;
    @FXML private TextArea txtNote;
    @FXML private CheckBox chkActive;
    @FXML private Button btnSave;

    private CustomerFormViewModel viewModel;
    private CustomerService customerService;
    private Runnable onSaveSuccess;

    public void initData(Customer customer, CustomerService service, Runnable onSuccess) {
        this.customerService = service;
        this.onSaveSuccess = onSuccess;
        this.viewModel = new CustomerFormViewModel(customer);

        lblTitle.setText(viewModel.isEditMode() ? "Sửa khách hàng" : "Thêm khách hàng mới");

        // Bindings
        txtName.textProperty().bindBidirectional(viewModel.nameProperty());
        txtPhone.textProperty().bindBidirectional(viewModel.phoneProperty());
        txtEmail.textProperty().bindBidirectional(viewModel.emailProperty());
        dpDateOfBirth.valueProperty().bindBidirectional(viewModel.dateOfBirthProperty());
        comboGender.valueProperty().bindBidirectional(viewModel.genderProperty());
        txtAddress.textProperty().bindBidirectional(viewModel.addressProperty());
        txtNote.textProperty().bindBidirectional(viewModel.noteProperty());
        chkActive.selectedProperty().bindBidirectional(viewModel.isActiveProperty());

        // Validation UI
        btnSave.disableProperty().bind(viewModel.isValidProperty().not());
        
        txtPhone.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && !newV.isEmpty() && !newV.matches("\\d{10}")) {
                lblPhoneError.setVisible(true);
                lblPhoneError.setManaged(true);
            } else {
                lblPhoneError.setVisible(false);
                lblPhoneError.setManaged(false);
            }
        });
    }

    @FXML
    private void handleSave() {
        Customer c = viewModel.getCustomer();
        Task<Void> saveTask = customerService.saveCustomerTask(c);
        
        saveTask.setOnSucceeded(e -> {
            if (onSaveSuccess != null) onSaveSuccess.run();
            closeStage();
        });
        saveTask.setOnFailed(e -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Lỗi khi lưu khách hàng: " + saveTask.getException().getMessage());
            alert.show();
        });
        
        new Thread(saveTask).start();
    }

    @FXML
    private void handleCancel() {
        closeStage();
    }

    private void closeStage() {
        Stage stage = (Stage) btnSave.getScene().getWindow();
        stage.close();
    }
}
