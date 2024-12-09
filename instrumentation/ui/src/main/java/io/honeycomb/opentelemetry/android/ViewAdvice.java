package io.honeycomb.opentelemetry.android;

import net.bytebuddy.asm.Advice;

public class ViewAdvice {
    @Advice.OnMethodEnter
    public static void enter() {
        System.out.println("hello test object");
    }
}
