package io.honeycomb.opentelemetry.android

import net.bytebuddy.asm.Advice
import net.bytebuddy.build.Plugin
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.ClassFileLocator
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.matcher.ElementMatchers
import java.io.IOException

class ViewPlugin : Plugin {
    override fun apply(
        builder: DynamicType.Builder<*>,
        typeDescription: TypeDescription,
        classFileLocator: ClassFileLocator
    ): DynamicType.Builder<*> {
        return builder.visit(
            Advice
                .to(ViewAdvice::class.java)
                .on(ElementMatchers.named("performClick"))
        )
    }

    @Throws(IOException::class)
    override fun close() {
    }

    override fun matches(target: TypeDescription): Boolean {
        if (target.typeName.startsWith("android.view")) {
            throw RuntimeException("got here: ${target.typeName}")
        }
        return target.typeName == "android.view.View"
    }
}
