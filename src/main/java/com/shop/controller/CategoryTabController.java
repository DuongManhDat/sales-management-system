package com.shop.controller;

import com.shop.model.Category;
import com.shop.service.CategoryService;
import com.shop.util.DialogHelper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Optional;

public class CategoryTabController {

    @FXML private TextField searchField;
    @FXML private TableView<Category> categoryTable;
    @FXML private TableColumn<Category, String> nameColumn;
    @FXML private TableColumn<Category, String> statusColumn;
    @FXML private TableColumn<Category, Void> actionColumn;
    @FXML private Label totalLabel;

    private CategoryService categoryService = new CategoryService();
    private ObservableList<Category> allCategories = FXCollections.observableArrayList();
    private FilteredList<Category> filteredCategories;

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        statusColumn.setCellValueFactory(cellData -> {
            int status = cellData.getValue().getStatus();
            return new SimpleStringProperty(status == 1 ? "ACTIVE" : "INACTIVE");
        });

        setupActionColumn();

        filteredCategories = new FilteredList<>(allCategories, c -> true);
        searchField.textProperty().addListener((obs, old, text) -> {
            filteredCategories.setPredicate(c -> 
                text == null || text.isBlank() || c.getName().toLowerCase().contains(text.toLowerCase())
            );
        });

        categoryTable.setItems(filteredCategories);
        
        loadData();
    }

    private void setupActionColumn() {
        actionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button(" Sửa", new FontIcon("fth-edit-2"));
            private final Button deleteBtn = new Button(" Xóa", new FontIcon("fth-trash-2"));
            private final HBox pane = new HBox(5, editBtn, deleteBtn);

            {
                editBtn.getStyleClass().addAll("button", "flat", "accent");
                deleteBtn.getStyleClass().addAll("button", "flat", "danger");

                editBtn.setOnAction(event -> {
                    Category category = getTableView().getItems().get(getIndex());
                    handleEditCategory(category);
                });
                deleteBtn.setOnAction(event -> {
                    Category category = getTableView().getItems().get(getIndex());
                    handleDeleteCategory(category);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void loadData() {
        allCategories.setAll(categoryService.findAllActive());
        totalLabel.setText("Tổng: " + allCategories.size() + " nhóm hàng");
    }

    @FXML
    public void handleAddCategory() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Thêm nhóm hàng");
        dialog.setHeaderText("Nhập tên nhóm hàng:");
        dialog.setContentText("Tên nhóm hàng:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            try {
                categoryService.add(name);
                loadData();
            } catch (Exception e) {
                DialogHelper.showError("Lỗi", e.getMessage());
            }
        });
    }

    private void handleEditCategory(Category category) {
        TextInputDialog dialog = new TextInputDialog(category.getName());
        dialog.setTitle("Sửa nhóm hàng");
        dialog.setHeaderText("Nhập tên mới:");
        dialog.setContentText("Tên nhóm hàng:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            try {
                categoryService.update(category.getId(), name);
                loadData();
            } catch (Exception e) {
                DialogHelper.showError("Lỗi", e.getMessage());
            }
        });
    }

    private void handleDeleteCategory(Category category) {
        DialogHelper.showConfirm("Xác nhận xóa", "Xóa nhóm hàng '" + category.getName() + "'? Thao tác này không thể hoàn tác.", () -> {
            try {
                categoryService.softDelete(category.getId());
                loadData();
            } catch (Exception e) {
                DialogHelper.showError("Lỗi", e.getMessage());
            }
        });
    }
}
