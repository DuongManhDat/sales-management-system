package com.shop.model.common;

import java.util.Collections;
import java.util.List;

/**
 * Đại diện cho kết quả phân trang dùng chung cho các màn hình.
 * @param <T> Kiểu dữ liệu của phần tử trong danh sách.
 */
public class PageResult<T> {
    private final List<T> items;
    private final int pageIndex;
    private final int pageSize;
    private final int totalItems;
    private final int totalPages;

    public PageResult(List<T> items, int pageIndex, int pageSize, int totalItems) {
        this.items = items != null ? items : Collections.emptyList();
        this.pageIndex = Math.max(1, pageIndex);
        this.pageSize = Math.max(1, pageSize);
        this.totalItems = Math.max(0, totalItems);
        this.totalPages = this.totalItems == 0 ? 1 : (int) Math.ceil((double) this.totalItems / this.pageSize);
    }

    public List<T> getItems() {
        return items;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean hasPreviousPage() {
        return pageIndex > 1;
    }

    public boolean hasNextPage() {
        return pageIndex < totalPages;
    }

    public int getFromIndex() {
        if (totalItems == 0) return 0;
        return (pageIndex - 1) * pageSize + 1;
    }

    public int getToIndex() {
        if (totalItems == 0) return 0;
        return Math.min(pageIndex * pageSize, totalItems);
    }
}

