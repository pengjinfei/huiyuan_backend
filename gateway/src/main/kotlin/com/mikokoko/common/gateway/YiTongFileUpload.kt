package com.mikokoko.common.gateway

import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpServerFileUpload
import io.vertx.ext.web.FileUpload

class YiTongFileUpload(var upload: HttpServerFileUpload ,var originBuffer: Buffer) : FileUpload {

    override fun name(): String {
        return upload.name()
    }

    override fun uploadedFileName(): String {
        return upload.filename()
    }

    override fun fileName(): String {
        return upload.filename()
    }

    override fun size(): Long {
        return upload.size()
    }

    override fun contentType(): String {
        return upload.contentType()
    }

    override fun contentTransferEncoding(): String {
        return upload.contentTransferEncoding()
    }

    override fun charSet(): String {
        return upload.charset()
    }

    override fun cancel(): Boolean {
        return upload.cancelStreamToFileSystem()
    }

}