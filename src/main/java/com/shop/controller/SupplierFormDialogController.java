package com.shop.controller;

import com.shop.model.Supplier;
import com.shop.service.SupplierService;
import com.shop.util.DialogHelper;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class SupplierFormDialogController {

    @FXML private Label lblTitle;
    @FXML private TextField txtCode;
    @FXML private TextField txtName;
    @FXML private TextField txtPhone;
    @FXML private TextField txtAddress;
    @FXML private TextArea txtNote;
    @FXML private CheckBox chkActive;
    @FXML private Button btnSave;

    private Supplier supplier;
    private boolean saved = false;
    private final SupplierService supplierService = new SupplierService();

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
        if (supplier.getId() != 0) {
            lblTitle.setText("Sửa nhà cung cấp");
            txtCode.setText(supplier.getCode());
            txtCode.setDisable(true); // Disable editing code after creation
            txtName.setText(supplier.getName());
            txtPhone.setText(supplier.getPhone());
            txtAddress.setText(supplier.getAddress());
            txtNote.setText(supplier.getNote());
            chkActive.setSelected(supplier.isActive());
        } else {
            lblTitle.setText("Thêm nhà cung cấp mới");
            chkActive.setSelected(true);
        }
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void handleSave() {
        supplier.setCode(txtCode.getText());
        supplier.setName(txtName.getText());
        supplier.setPhone(txtPhone.getText());
        supplier.setAddress(txtAddress.getText());
        supplier.setNote(txtNote.getText());
        supplier.setActive(chkActive.isSelected());

        btnSave.setDisable(true);

        Task<Void> task = supplierService.saveSupplierTask(supplier);
        task.setOnSucceeded(e -> {
            saved = true;
            closeStage();
        });
        task.setOnFailed(e -> {
            btnSave.setDisable(false);
            DialogHelper.showError("Lỗi", task.getException().getMessage());
        });

        new Thread(task).start();
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
