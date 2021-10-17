package com.mikokoko.common.gateway

import io.netty.handler.codec.DecoderException
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.impl.RoutingContextInternal
import java.util.concurrent.atomic.AtomicInteger

class YiTongBodyHandler : BodyHandler {

    private val LOG = LoggerFactory.getLogger(BodyHandler::class.java)

    private var bodyLimit = BodyHandler.DEFAULT_BODY_LIMIT
    private var mergeFormAttributes = BodyHandler.DEFAULT_MERGE_FORM_ATTRIBUTES
    private var isPreallocateBodyBuffer = BodyHandler.DEFAULT_PREALLOCATE_BODY_BUFFER
    private val DEFAULT_INITIAL_BODY_BUFFER_SIZE = 1024

    override fun handle(context: RoutingContext?) {
        if (context == null) {
            throw IllegalArgumentException("")
        }
        val request: HttpServerRequest = context.request()
        if (request.headers().contains(HttpHeaders.UPGRADE, HttpHeaders.WEBSOCKET, true)) {
            context.next()
            return
        }
        // we need to keep state since we can be called again on reroute
        // we need to keep state since we can be called again on reroute
        if (!(context as RoutingContextInternal).seenHandler(RoutingContextInternal.BODY_HANDLER)) {
            val contentLength: Long = if (isPreallocateBodyBuffer) parseContentLengthHeader(request) else -1
            val handler = YiTongBHandler(context, contentLength)
            request.handler(handler)
            request.endHandler { v: Void? -> handler.end() }
            (context as RoutingContextInternal).visitHandler(RoutingContextInternal.BODY_HANDLER)
        } else {
            // on reroute we need to re-merge the form params if that was desired
            if (mergeFormAttributes && request.isExpectMultipart) {
                request.params().addAll(request.formAttributes())
            }
            context.next()
        }
    }

    private fun parseContentLengthHeader(request: HttpServerRequest): Long {
        val contentLength = request.getHeader(HttpHeaders.CONTENT_LENGTH)
        return if (contentLength == null || contentLength.isEmpty()) {
            -1
        } else try {
            val parsedContentLength = contentLength.toLong()
            if (parsedContentLength < 0) -1 else parsedContentLength
        } catch (ex: NumberFormatException) {
            -1
        }
    }

    override fun setHandleFileUploads(handleFileUploads: Boolean): BodyHandler {
        return this
    }

    override fun setBodyLimit(bodyLimit: Long): BodyHandler{
        this.bodyLimit = bodyLimit
        return this
    }

    override fun setUploadsDirectory(uploadsDirectory: String): BodyHandler {
        return this
    }

    override fun setMergeFormAttributes(mergeFormAttributes: Boolean): BodyHandler {
        this.mergeFormAttributes = mergeFormAttributes
        return this
    }

    override fun setDeleteUploadedFilesOnEnd(deleteUploadedFilesOnEnd: Boolean): BodyHandler {
        return this
    }

    override fun setPreallocateBodyBuffer(isPreallocateBodyBuffer: Boolean): BodyHandler {
        this.isPreallocateBodyBuffer = isPreallocateBodyBuffer
        return this
    }

    inner class YiTongBHandler(val context: RoutingContext, private val contentLength: Long) : Handler<Buffer> {
        private val MAX_PREALLOCATED_BODY_BUFFER_BYTES = 65535

        var body: Buffer? = null
        var failed = false
        var uploadCount = AtomicInteger()
        var ended = false
        var uploadSize = 0L
        private var isMultipart = false
        private var isUrlEncoded = false


        init {
            if (contentLength != -1L) {
                initBodyBuffer()
            }

            val fileUploads = context.fileUploads()

            val contentType = context.request().getHeader(HttpHeaders.CONTENT_TYPE)
            if (contentType == null) {
                isMultipart = false
                isUrlEncoded = false
            } else {
                val lowerCaseContentType = contentType.toLowerCase()
                isMultipart = lowerCaseContentType.startsWith(HttpHeaderValues.MULTIPART_FORM_DATA.toString())
                isUrlEncoded =
                    lowerCaseContentType.startsWith(HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString())
            }

            if (isMultipart || isUrlEncoded) {
                context.request().isExpectMultipart = true
                context.request().uploadHandler { upload ->
                    if (bodyLimit != -1L && upload.isSizeAvailable) {
                        // we can try to abort even before the upload starts
                        val size: Long = uploadSize + upload.size()
                        if (size > bodyLimit) {
                            failed = true
                            context.fail(413)
                            return@uploadHandler
                        }
                    }
                    uploadCount.incrementAndGet()
                    upload.handler {
                        val fileUpload = YiTongFileUpload(upload,it)
                        fileUploads.add(fileUpload)
                        uploadEnded()
                    }
                }
            }

            context.request().exceptionHandler { t: Throwable ->
                if (t is DecoderException) {
                    // bad request
                    context.fail(400, t.cause)
                } else {
                    context.fail(t)
                }
            }

        }

        private fun initBodyBuffer() {
            var initialBodyBufferSize: Int
            initialBodyBufferSize = if (contentLength < 0) {
                DEFAULT_INITIAL_BODY_BUFFER_SIZE
            } else if (contentLength > MAX_PREALLOCATED_BODY_BUFFER_BYTES) {
                MAX_PREALLOCATED_BODY_BUFFER_BYTES
            } else {
                contentLength.toInt()
            }
            if (bodyLimit != -1L) {
                initialBodyBufferSize = Math.min(initialBodyBufferSize.toLong(), bodyLimit).toInt()
            }
            body = Buffer.buffer(initialBodyBufferSize)
        }

        override fun handle(buff: Buffer) {
            if (failed) {
                return
            }
            uploadSize += buff.length().toLong()
            if (bodyLimit != -1L && uploadSize > bodyLimit) {
                failed = true
                context.fail(413)
            } else {
                // multipart requests will not end up in the request body
                // url encoded should also not, however jQuery by default
                // post in urlencoded even if the payload is something else
                if (!isMultipart /* && !isUrlEncoded */) {
                    if (body == null) {
                        initBodyBuffer()
                    }
                    body?.appendBuffer(buff)
                }
            }
        }

        fun uploadEnded() {
            val count: Int = uploadCount.decrementAndGet()
            // only if parsing is done and count is 0 then all files have been processed
            if (ended && count == 0) {
                doEnd()
            }
        }

        fun end() {
            // this marks the end of body parsing, calling doEnd should
            // only be possible from this moment onwards
            ended = true

            // only if parsing is done and count is 0 then all files have been processed
            if (uploadCount.get() == 0) {
                doEnd()
            }
        }

        fun doEnd() {
            if (failed) {
                return
            }
            val req: HttpServerRequest = context.request()
            if (mergeFormAttributes && req.isExpectMultipart) {
                req.params().addAll(req.formAttributes())
            }
            context.setBody(body)
            // release body as it may take lots of memory
            body = null
            context.next()
        }
    }
}