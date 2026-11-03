package com.shop.control;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.function.Consumer;

/**
 * Component phân trang dùng chung cho các TableView trong ứng dụng.
 * Thiết kế theo phong cách Industrial Brutalist / Flat, tương phản cao, trực quan.
 */
public class PaginationControl extends HBox {

    private final Label lblInfo;
    private final HBox buttonBox;
    private final Button btnFirst;
    private final Button btnPrev;
    private final Button btnNext;
    private final Button btnLast;

    private int currentPage = 1;
    private int pageSize = 15;
    private int totalItems = 0;
    private int totalPages = 1;

    private Consumer<Integer> onPageChangeListener;

    public PaginationControl() {
        this(15);
    }

    public PaginationControl(int defaultPageSize) {
        this.pageSize = Math.max(1, defaultPageSize);

        getStyleClass().add("pagination-control");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(12);

        // Label thông tin hiển thị bên trái
        lblInfo = new Label("Hiển thị 0 - 0 của 0 bản ghi");
        lblInfo.getStyleClass().add("pagination-info-label");

        // Spacer đẩy cụm nút phân trang sang bên phải
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // HBox chứa cụm nút
        buttonBox = new HBox(4);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        btnFirst = createNavButton("|<", "btn-pagination-nav");
        btnPrev = createNavButton("<", "btn-pagination-nav");
        btnNext = createNavButton(">", "btn-pagination-nav");
        btnLast = createNavButton(">|", "btn-pagination-nav");

        btnFirst.setOnAction(e -> goToPage(1));
        btnPrev.setOnAction(e -> goToPage(currentPage - 1));
        btnNext.setOnAction(e -> goToPage(currentPage + 1));
        btnLast.setOnAction(e -> goToPage(totalPages));

        getChildren().addAll(lblInfo, spacer, buttonBox);

        updateUI();
    }

    private Button createNavButton(String text, String styleClass) {
        Button btn = new Button(text);
        btn.getStyleClass().addAll("btn-pagination", styleClass);
        btn.setFocusTraversable(false);
        return btn;
    }

    public void setOnPageChangeListener(Consumer<Integer> listener) {
        this.onPageChangeListener = listener;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = Math.max(1, pageSize);
        recalculateAndRender();
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void updateState(int currentPage, int pageSize, int totalItems) {
        this.currentPage = Math.max(1, currentPage);
        this.pageSize = Math.max(1, pageSize);
        this.totalItems = Math.max(0, totalItems);
        this.totalPages = this.totalItems == 0 ? 1 : (int) Math.ceil((double) this.totalItems / this.pageSize);
        if (this.currentPage > this.totalPages) {
            this.currentPage = this.totalPages;
        }
        updateUI();
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = Math.max(0, totalItems);
        recalculateAndRender();
    }

    private void recalculateAndRender() {
        this.totalPages = this.totalItems == 0 ? 1 : (int) Math.ceil((double) this.totalItems / this.pageSize);
        if (this.currentPage > this.totalPages) {
            this.currentPage = this.totalPages;
        }
        updateUI();
    }

    private void goToPage(int page) {
        if (page < 1 || page > totalPages || page == currentPage) {
            return;
        }
        this.currentPage = page;
        updateUI();
        if (onPageChangeListener != null) {
            onPageChangeListener.accept(currentPage);
        }
    }

    private void updateUI() {
        // Cập nhật text thông tin
        if (totalItems == 0) {
            lblInfo.setText("Hiển thị 0 - 0 của 0 bản ghi");
        } else {
            int from = (currentPage - 1) * pageSize + 1;
            int to = Math.min(currentPage * pageSize, totalItems);
            lblInfo.setText(String.format("Hiển thị %d - %d của %d bản ghi", from, to, totalItems));
        }

        // Render các nút
        buttonBox.getChildren().clear();

        btnFirst.setDisable(currentPage <= 1 || totalPages <= 1);
        btnPrev.setDisable(currentPage <= 1 || totalPages <= 1);
        btnNext.setDisable(currentPage >= totalPages || totalPages <= 1);
        btnLast.setDisable(currentPage >= totalPages || totalPages <= 1);

        buttonBox.getChildren().addAll(btnFirst, btnPrev);

        // Hiển thị các trang xung quanh currentPage (tối đa 5 nút số)
        int startPage = Math.max(1, currentPage - 2);
        int endPage = Math.min(totalPages, startPage + 4);
        if (endPage - startPage < 4) {
            startPage = Math.max(1, endPage - 4);
        }

        if (startPage > 1) {
            Label dots = new Label("...");
            dots.getStyleClass().add("pagination-dots");
            buttonBox.getChildren().add(dots);
        }

        for (int i = startPage; i <= endPage; i++) {
            final int pageNumber = i;
            Button pageBtn = new Button(String.valueOf(pageNumber));
            pageBtn.getStyleClass().add("btn-pagination");
            if (pageNumber == currentPage) {
                pageBtn.getStyleClass().add("btn-pagination-active");
            }
            pageBtn.setOnAction(e -> goToPage(pageNumber));
            buttonBox.getChildren().add(pageBtn);
        }

        if (endPage < totalPages) {
            Label dots = new Label("...");
            dots.getStyleClass().add("pagination-dots");
            buttonBox.getChildren().add(dots);
        }

        buttonBox.getChildren().addAll(btnNext, btnLast);
    }
}

