package com.mikokoko.common.gateway

import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.Message
import io.vertx.ext.web.RoutingContext

class Http2MsgHandler : Handler<RoutingContext> {
    override fun handle(context: RoutingContext) {
        val vertx = context.vertx()

        val request = context.request()
        val uri = request.uri()

        val address = "merchant"

        val deliveryOptions = DeliveryOptions()
        deliveryOptions.sendTimeout = 3000
        deliveryOptions.addHeader("uri", uri.removePrefix("/merchant"))
        val body = context.body
        if (body == null) {
            var fileUploads = context.fileUploads()

        } else {
            vertx.eventBus().request<Any>(address, body, deliveryOptions) {
                replyHandler(it, context)
            }
        }

    }

    private fun replyHandler(it: AsyncResult<Message<Any>>, request: RoutingContext) {
        if (it.failed()) {
            request.fail(it.cause())
            return
        }
        val result = it.result()
        val upstreamBody = result.body()
        if (upstreamBody is String) {
            val response = request.response();
            response.putHeader("content-type", "application/json;charset=UTF-8")
            response.send(upstreamBody)
        }
    }

}