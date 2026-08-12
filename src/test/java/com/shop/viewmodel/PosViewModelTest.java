package com.shop.viewmodel;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PosViewModelTest {
    @Test
    public void testPosViewModel() {
        PosViewModel vm = new PosViewModel();
        assertNotNull(vm);
    }
}
