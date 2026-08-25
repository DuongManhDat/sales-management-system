package com.shop.controller;

import com.shop.dao.InvoiceDao;
import com.shop.dao.InvoiceItemDao;
import com.shop.model.Invoice;
import com.shop.model.InvoiceItem;
import com.shop.model.ReturnInvoice;
import com.shop.model.ReturnInvoiceItem;
import com.shop.service.SalesService;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReturnFormController {
    private static final Logger log = LoggerFactory.getLogger(ReturnFormController.class);

    @FXML private TextField txtInvoiceId;
    @FXML private Label lblCustomer;
    @FXML private Label lblOriginalTotal;
    
    @FXML private TableView<InvoiceItem> itemTable;
    @FXML private TableColumn<InvoiceItem, String> colProductId;
    @FXML private TableColumn<InvoiceItem, Number> colQty;
    @FXML private TableColumn<InvoiceItem, Number> colPrice;

    @FXML private Label lblSelectedProduct;
    @FXML private TextField txtReturnQty;
    @FXML private TextField txtReturnFee;
    @FXML private Label lblTotalRefund;

    private InvoiceDao invoiceDao = new InvoiceDao();
    private InvoiceItemDao invoiceItemDao = new InvoiceItemDao();
    private SalesService salesService = new SalesService();
    
    private Invoice currentInvoice;
    // Map to keep track of how many items customer wants to return
    private Map<Integer, Integer> returnQtyMap = new HashMap<>(); // invoiceItemId -> returnQty

    @FXML
    public void initialize() {
        colProductId.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getProductId())));
        colQty.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().getQty()));
        colPrice.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().getSalePrice()));
        
        itemTable.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            if (val != null) {
                lblSelectedProduct.setText("ID: " + val.getProductId() + " (Đã mua: " + val.getQty() + ")");
                txtReturnQty.setText(String.valueOf(returnQtyMap.getOrDefault(val.getId(), 0)));
            }
        });

        txtReturnFee.textProperty().addListener((obs, old, val) -> calculateRefund());
    }

    @FXML
    private void loadInvoiceData() {
        try {
            int id = Integer.parseInt(txtInvoiceId.getText());
            currentInvoice = invoiceDao.findById(id);
            if (currentInvoice == null) {
                showAlert("Lỗi", "Không tìm thấy hóa đơn có ID = " + id);
                return;
            }
            lblCustomer.setText(String.valueOf(currentInvoice.getCustomerId()));
            lblOriginalTotal.setText(String.valueOf(currentInvoice.getTotal()));
            
            List<InvoiceItem> items = invoiceItemDao.findByInvoiceId(id);
            itemTable.setItems(FXCollections.observableArrayList(items));
            returnQtyMap.clear();
            calculateRefund();
        } catch (NumberFormatException e) {
            showAlert("Lỗi", "ID hóa đơn phải là số nguyên");
        } catch (Exception e) {
            log.error("Failed to load invoice", e);
        }
    }

    @FXML
    private void confirmReturnQty() {
        InvoiceItem selected = itemTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Lỗi", "Vui lòng chọn sản phẩm trong bảng");
            return;
        }
        try {
            int returnQty = Integer.parseInt(txtReturnQty.getText());
            if (returnQty < 0 || returnQty > selected.getQty()) {
                showAlert("Lỗi", "Số lượng trả không hợp lệ (0 - " + selected.getQty() + ")");
                return;
            }
            returnQtyMap.put(selected.getId(), returnQty);
            calculateRefund();
        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Số lượng phải là số nguyên");
        }
    }

    private void calculateRefund() {
        if (currentInvoice == null) return;
        long grossRefund = 0;
        for (InvoiceItem item : itemTable.getItems()) {
            int qty = returnQtyMap.getOrDefault(item.getId(), 0);
            grossRefund += qty * item.getSalePrice();
        }
        
        long fee = 0;
        try {
            fee = Long.parseLong(txtReturnFee.getText());
        } catch (NumberFormatException ignored) {}
        
        long netRefund = grossRefund - fee;
        lblTotalRefund.setText(String.valueOf(netRefund));
    }

    @FXML
    private void submitReturn() {
        if (currentInvoice == null) {
            showAlert("Lỗi", "Vui lòng tải hóa đơn trước");
            return;
        }
        
        long grossRefund = 0;
        List<ReturnInvoiceItem> returnItems = new ArrayList<>();
        for (InvoiceItem item : itemTable.getItems()) {
            int qty = returnQtyMap.getOrDefault(item.getId(), 0);
            if (qty > 0) {
                ReturnInvoiceItem riItem = new ReturnInvoiceItem();
                riItem.setInvoiceItemId(item.getId());
                riItem.setProductId(item.getProductId());
                riItem.setReturnQty(qty);
                riItem.setRefundPrice(item.getSalePrice());
                riItem.setAmount(qty * item.getSalePrice());
                returnItems.add(riItem);
                
                grossRefund += riItem.getAmount();
            }
        }
        
        if (returnItems.isEmpty()) {
            showAlert("Lỗi", "Bạn chưa chọn sản phẩm nào để trả");
            return;
        }

        long fee = 0;
        try {
            fee = Long.parseLong(txtReturnFee.getText());
        } catch (NumberFormatException ignored) {}

        ReturnInvoice ri = new ReturnInvoice();
        ri.setInvoiceId(currentInvoice.getId());
        ri.setCustomerId(currentInvoice.getCustomerId());
        ri.setTotalRefund(grossRefund); // Gross amount before fee
        ri.setReturnFee(fee);

        try {
            salesService.processReturn(ri, returnItems);
            showAlert("Thành công", "Phiếu trả hàng đã được tạo!");
            cancel(); // close or reset
        } catch (Exception e) {
            log.error("Lỗi khi tạo phiếu trả hàng", e);
            showAlert("Lỗi", "Không thể tạo phiếu: " + e.getMessage());
        }
    }

    @FXML
    private void cancel() {
        com.shop.util.SceneManager.switchScene("/fxml/return-list.fxml", "Danh sách Phiếu Trả Hàng");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
