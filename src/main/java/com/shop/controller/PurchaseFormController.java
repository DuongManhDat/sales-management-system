package com.shop.controller;

import javafx.fxml.FXML;

public class PurchaseFormController {
    
    @FXML private javafx.scene.control.Label lblTotal;

    @FXML
    public void handleBack() {
        // Logic close stage (nếu dùng modal)
        javafx.stage.Stage stage = null;
        if (lblTotal != null && lblTotal.getScene() != null) {
            stage = (javafx.stage.Stage) lblTotal.getScene().getWindow();
        }
        if (stage != null) {
            stage.close();
        }
    }

    @FXML
    public void handleAddLine() {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText("Tính năng chọn sản phẩm đang được xây dựng ở Phase tiếp theo!");
        alert.showAndWait();
    }

    @FXML
    public void handleSave() {}
}
