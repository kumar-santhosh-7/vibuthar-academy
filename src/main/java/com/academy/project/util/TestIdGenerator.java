package com.academy.project.util;

import com.academy.project.entity.test.OnlineTest;

public final class TestIdGenerator {

    private TestIdGenerator() {
    }

    public static String generate(OnlineTest test) {
        return "TST" + String.format("%06d", test.getId());
    }
}
