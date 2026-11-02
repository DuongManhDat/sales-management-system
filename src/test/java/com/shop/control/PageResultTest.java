package com.shop.control;

import com.shop.model.common.PageResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PageResultTest {

    @Test
    @DisplayName("Kiểm tra tính toán totalPages và metadata trong PageResult")
    public void testPageResultMetadata() {
        List<String> items = List.of("Item 1", "Item 2", "Item 3");
        PageResult<String> result = new PageResult<>(items, 1, 15, 42);

        assertEquals(1, result.getPageIndex());
        assertEquals(15, result.getPageSize());
        assertEquals(42, result.getTotalItems());
        assertEquals(3, result.getTotalPages());
        assertEquals(3, result.getItems().size());
        assertTrue(result.hasNextPage());
        assertFalse(result.hasPreviousPage());
    }

    @Test
    @DisplayName("Kiểm tra phân trang khi số lượng bản ghi là 0")
    public void testPageResultEmpty() {
        PageResult<String> result = new PageResult<>(List.of(), 1, 15, 0);

        assertEquals(1, result.getPageIndex());
        assertEquals(0, result.getTotalItems());
        assertEquals(1, result.getTotalPages()); // Ít nhất 1 trang hiển thị
        assertFalse(result.hasNextPage());
        assertFalse(result.hasPreviousPage());
    }

    @Test
    @DisplayName("Kiểm tra tính toán trang giữa và trang cuối")
    public void testPageResultMiddleAndEnd() {
        PageResult<String> page2 = new PageResult<>(List.of("A"), 2, 10, 25);
        assertTrue(page2.hasPreviousPage());
        assertTrue(page2.hasNextPage());
        assertEquals(3, page2.getTotalPages());

        PageResult<String> page3 = new PageResult<>(List.of("B"), 3, 10, 25);
        assertTrue(page3.hasPreviousPage());
        assertFalse(page3.hasNextPage());
    }
}

