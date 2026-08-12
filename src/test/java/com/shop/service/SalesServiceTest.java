package com.shop.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SalesServiceTest {
    @Test
    public void testSalesService() {
        SalesService service = new SalesService();
        assertNotNull(service);
    }
}
