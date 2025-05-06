package io.honeycomb.opentelemetry.android.example

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.logs.LogRecordProcessor
import io.opentelemetry.sdk.logs.ReadWriteLogRecord

class SimpleLogRecordProcessor : LogRecordProcessor {
    override fun onEmit(
        context: Context,
        logRecord: ReadWriteLogRecord,
    ) {
        logRecord.setAttribute(AttributeKey.stringKey("app.metadata"), "extra metadata")
    }
}
