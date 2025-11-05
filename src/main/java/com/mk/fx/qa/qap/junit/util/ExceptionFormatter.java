package com.mk.fx.qa.qap.junit.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

public final class ExceptionFormatter {

    private ExceptionFormatter() {}

    public static byte[] toBytes(String text) {
        if (text == null) {
            return new byte[0];
        }
        return text.getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] toBytes(Throwable throwable) {
        if (throwable == null) {
            return new byte[0];
        }
        return toBytes(stackTraceOf(throwable));
    }

    public static String stackTraceOf(Throwable throwable) {
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}

