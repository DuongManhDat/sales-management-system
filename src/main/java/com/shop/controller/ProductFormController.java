package com.shop.controller;

import com.shop.dao.CategoryDao;
import com.shop.dao.UnitDao;
import com.shop.model.Category;
import com.shop.model.Product;
import com.shop.model.Unit;
import com.shop.service.ProductService;
import com.shop.viewmodel.ProductFormViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

public class ProductFormController {

    private static final Logger log = LoggerFactory.getLogger(ProductFormController.class);

    @FXML private Label lblTitle;
    @FXML private TextField txtCode;
    @FXML private TextField txtName;
    @FXML private ComboBox<Unit> comboUnit;
    @FXML private ComboBox<Category> comboCategory;
    @FXML private TextField txtPrice;
    @FXML private TextArea txtNote;

    private ProductFormViewModel viewModel = new ProductFormViewModel();
    private ProductService productService = new ProductService();
    private UnitDao unitDao = new UnitDao();
    private CategoryDao categoryDao = new CategoryDao();

    private Product currentProduct;

    @FXML
    public void initialize() {
        setupComboBoxes();
        
        // Bindings
        txtCode.textProperty().bindBidirectional(viewModel.codeProperty());
        txtName.textProperty().bindBidirectional(viewModel.nameProperty());
        txtNote.textProperty().bindBidirectional(viewModel.noteProperty());
        
        // Custom binding for numbers
        txtPrice.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.matches("\\d*")) {
                txtPrice.setText(newV.replaceAll("[^\\d]", ""));
            } else if (!newV.isEmpty()) {
                viewModel.salePriceProperty().set(Long.parseLong(newV));
            }
        });
        
        viewModel.salePriceProperty().addListener((obs, oldV, newV) -> {
            if (!txtPrice.getText().equals(newV.toString())) {
                txtPrice.setText(newV.toString());
            }
        });
    }

    private void setupComboBoxes() {
        try {
            List<Unit> units = unitDao.findAllActive();
            comboUnit.getItems().setAll(units);
            comboUnit.setConverter(new StringConverter<>() {
                @Override public String toString(Unit object) { return object != null ? object.getName() : ""; }
                @Override public Unit fromString(String string) { return null; }
            });
            
            comboUnit.valueProperty().addListener((obs, oldV, newV) -> {
                if (newV != null) viewModel.unitIdProperty().set(newV.getId());
            });

            List<Category> categories = categoryDao.findAllActive();
            comboCategory.getItems().setAll(categories);
            comboCategory.setConverter(new StringConverter<>() {
                @Override public String toString(Category object) { return object != null ? object.getName() : ""; }
                @Override public Category fromString(String string) { return null; }
            });
            
            comboCategory.valueProperty().addListener((obs, oldV, newV) -> {
                if (newV != null) viewModel.categoryIdProperty().set(newV.getId());
            });
        } catch (Exception e) {
            log.error("Lỗi khi tải dữ liệu đơn vị tính hoặc danh mục: {}", e.getMessage(), e);
        }
    }

    public void setProduct(Product product) {
        this.currentProduct = product;
        if (product != null) {
            lblTitle.setText("Sửa Hàng Hóa");
            viewModel.loadFromProduct(product);
            
            // Set combo selection
            comboUnit.getItems().stream().filter(u -> u.getId() == product.getUnitId()).findFirst().ifPresent(comboUnit::setValue);
            comboCategory.getItems().stream().filter(c -> c.getId() == product.getCategoryId()).findFirst().ifPresent(comboCategory::setValue);
        } else {
            lblTitle.setText("Thêm Hàng Hóa");
            viewModel.reset();
        }
    }

    @FXML
    private void handleSave() {
        if (txtName.getText() == null || txtName.getText().trim().isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập tên hàng hóa.");
            return;
        }

        try {
            if (currentProduct == null) {
                Product newProduct = new Product();
                viewModel.updateProduct(newProduct);
                log.info("Đang thêm mới hàng hóa: name='{}', unitId={}, categoryId={}, salePrice={}",
                        newProduct.getName(), newProduct.getUnitId(), newProduct.getCategoryId(), newProduct.getSalePrice());
                int createdId = productService.addProduct(newProduct);
                log.info("Thêm mới hàng hóa thành công với ID={}", createdId);
            } else {
                viewModel.updateProduct(currentProduct);
                log.info("Đang cập nhật hàng hóa ID={}, code='{}', name='{}'",
                        currentProduct.getId(), currentProduct.getCode(), currentProduct.getName());
                productService.updateProduct(currentProduct);
                log.info("Cập nhật hàng hóa ID={} thành công", currentProduct.getId());
            }
            closeDialog();
        } catch (SQLException e) {
            log.error("Lỗi cơ sở dữ liệu khi lưu hàng hóa: {}", e.getMessage(), e);
            showAlert("Lỗi", "Không thể lưu hàng hóa: " + e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi lưu hàng hóa: {}", e.getMessage(), e);
            showAlert("Lỗi", "Đã xảy ra lỗi: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) txtName.getScene().getWindow();
        stage.close();
    }
    
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
