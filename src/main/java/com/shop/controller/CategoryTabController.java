package com.shop.controller;

import com.shop.model.Category;
import com.shop.control.PaginationControl;
import com.shop.model.common.PageResult;
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
    @FXML private PaginationControl paginationControl;
    private int currentPage = 1;
    private static final int PAGE_SIZE = 15;
    @FXML private Label totalLabel;

    private CategoryService categoryService = new CategoryService();
    private ObservableList<Category> allCategories = FXCollections.observableArrayList();
    private FilteredList<Category> filteredCategories;

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        statusColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(com.shop.model.ActiveStatus.fromInt(cellData.getValue().getStatus()).getDisplayName())
        );
        statusColumn.setCellFactory(com.shop.util.StatusBadge.createCellFactory());

        setupActionColumn();
        categoryTable.setItems(allCategories);

        paginationControl.setPageSize(PAGE_SIZE);
        paginationControl.setOnPageChangeListener(page -> {
            this.currentPage = page;
            loadData();
        });

        searchField.textProperty().addListener((obs, old, text) -> {
            currentPage = 1;
            loadData();
        });
        
        loadData();
    }

    private void setupActionColumn() {
        actionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button();
            private final Button deleteBtn = new Button();
            private final FontIcon editIcon = new FontIcon("fth-edit-2");
            private final FontIcon deleteIcon = new FontIcon("fth-trash-2");
            private final Tooltip editTooltip = new Tooltip("Sửa nhóm hàng");
            private final Tooltip deleteTooltip = new Tooltip("Xóa nhóm hàng");
            private final HBox pane = new HBox(6, editBtn, deleteBtn);

            {
                pane.setAlignment(javafx.geometry.Pos.CENTER);
                editBtn.setGraphic(editIcon);
                editBtn.setTooltip(editTooltip);
                editBtn.getStyleClass().addAll("btn-icon-action", "btn-icon-edit");

                deleteBtn.setGraphic(deleteIcon);
                deleteBtn.setTooltip(deleteTooltip);
                deleteBtn.getStyleClass().addAll("btn-icon-action", "btn-icon-delete");

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

        // Double click vào hàng để sửa nhóm hàng
        categoryTable.setRowFactory(tv -> {
            TableRow<Category> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    handleEditCategory(row.getItem());
                }
            });
            return row;
        });
    }

    private void loadData() {
        String keyword = searchField != null ? searchField.getText() : null;
        PageResult<Category> result = categoryService.findPaged(keyword, currentPage, PAGE_SIZE);
        allCategories.setAll(result.getItems());
        if (totalLabel != null) {
            totalLabel.setText("Tổng: " + result.getTotalItems() + " nhóm hàng");
        }
        if (paginationControl != null) {
            paginationControl.updateState(result.getPageIndex(), result.getPageSize(), result.getTotalItems());
        }
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
