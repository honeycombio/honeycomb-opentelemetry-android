package io.honeycomb.opentelemetry.android

import net.bytebuddy.asm.Advice
import net.bytebuddy.build.Plugin
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.ClassFileLocator
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.matcher.ElementMatchers
import java.io.IOException

class TestPlugin : Plugin {
    override fun apply(
        builder: DynamicType.Builder<*>,
        typeDescription: TypeDescription,
        classFileLocator: ClassFileLocator
    ): DynamicType.Builder<*> {
        return builder.visit(
            Advice
                .to(TestObjectAdvice::class.java)
                .on(ElementMatchers.named("getValue"))
        )
    }

    @Throws(IOException::class)
    override fun close() {
    }

    override fun matches(target: TypeDescription): Boolean {
        return target.typeName == "io.honeycomb.opentelemetry.android.TestObject"
    }
}
