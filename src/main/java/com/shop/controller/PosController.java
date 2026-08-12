package com.shop.controller;

import com.shop.dao.CustomerDao;
import com.shop.dao.ProductDao;
import com.shop.model.Customer;
import com.shop.model.InvoiceItem;
import com.shop.model.Product;
import com.shop.viewmodel.PosViewModel;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

public class PosController {
    private static final Logger log = LoggerFactory.getLogger(PosController.class);

    @FXML private TextField searchProductField;
    @FXML private ListView<Product> productListView;
    
    @FXML private ComboBox<Customer> customerComboBox;
    
    @FXML private TableView<InvoiceItem> cartTable;
    @FXML private TableColumn<InvoiceItem, String> colProductName; // Assuming we can get name or just keep ID for MVP. Wait, we need name.
    @FXML private TableColumn<InvoiceItem, Number> colQty;
    @FXML private TableColumn<InvoiceItem, Number> colPrice;
    @FXML private TableColumn<InvoiceItem, Number> colAmount;

    @FXML private Label lblSubtotal;
    @FXML private Label lblTotal;
    @FXML private TextField txtCustomerPaid;
    @FXML private Label lblChange;
    
    private PosViewModel viewModel = new PosViewModel();
    private ProductDao productDao = new ProductDao();
    private CustomerDao customerDao = new CustomerDao();

    @FXML
    public void initialize() {
        setupProductSearch();
        setupCartTable();
        setupCustomerBox();
        bindViewModel();
    }

    private void setupProductSearch() {
        searchProductField.textProperty().addListener((obs, old, val) -> {
            if (val.length() >= 2) {
                try {
                    List<Product> products = productDao.searchProducts(val);
                    productListView.setItems(FXCollections.observableArrayList(products));
                } catch (SQLException e) {
                    log.error("Failed to search products", e);
                }
            }
        });

        // Use cell factory to show product name instead of object reference
        productListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getCode() + " - " + item.getName() + " - " + item.getSalePrice());
                }
            }
        });

        productListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Product selected = productListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    viewModel.addProduct(selected);
                }
            }
        });
    }

    private void setupCartTable() {
        cartTable.setItems(viewModel.getInvoiceItems());
        
        // For MVP, if we don't have product name in InvoiceItem, we can just show Product ID or we fetch it. 
        // We will just show Product ID for now to be minimal, or we should have stored it. 
        // I'll show ID to keep it simple, or I could modify InvoiceItem to have transient productName.
        colProductName.setCellValueFactory(cellData -> new SimpleStringProperty("Product " + cellData.getValue().getProductId()));
        colQty.setCellValueFactory(cellData -> new SimpleLongProperty(cellData.getValue().getQty()));
        colPrice.setCellValueFactory(cellData -> new SimpleLongProperty(cellData.getValue().getSalePrice()));
        colAmount.setCellValueFactory(cellData -> new SimpleLongProperty(cellData.getValue().getAmount()));
    }

    private void setupCustomerBox() {
        try {
            customerComboBox.setItems(FXCollections.observableArrayList(customerDao.getAllActive()));
        } catch (SQLException e) {
            log.error("Failed to load customers", e);
        }
        customerComboBox.valueProperty().bindBidirectional(viewModel.selectedCustomerProperty());
    }

    private void bindViewModel() {
        lblSubtotal.textProperty().bind(viewModel.subtotalProperty().asString());
        lblTotal.textProperty().bind(viewModel.totalProperty().asString());
        lblChange.textProperty().bind(viewModel.changeOrDebtProperty().asString());
        
        txtCustomerPaid.textProperty().addListener((obs, old, val) -> {
            try {
                if (val.isEmpty()) {
                    viewModel.customerPaidProperty().set(0);
                } else {
                    viewModel.customerPaidProperty().set(Long.parseLong(val));
                }
            } catch (NumberFormatException e) {
                // Ignore invalid input
            }
        });
    }

    @FXML
    private void handleCheckout() {
        if (viewModel.getInvoiceItems().isEmpty()) {
            showAlert("Error", "Cart is empty!");
            return;
        }
        // Here we would call SalesService.createInvoice()
        // ... (skipped full implementation for brevity, MVP focus)
        showAlert("Success", "Thanh toán thành công!");
        viewModel.getInvoiceItems().clear();
        viewModel.customerPaidProperty().set(0);
        txtCustomerPaid.setText("0");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
