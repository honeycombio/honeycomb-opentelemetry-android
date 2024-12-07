package io.honeycomb.opentelemetry.android;

import net.bytebuddy.asm.Advice;

public class TestObjectAdvice {
    @Advice.OnMethodEnter
    public static void enter(@Advice.This TestObject obj) {
        System.out.println("hello test object");
    }
}
