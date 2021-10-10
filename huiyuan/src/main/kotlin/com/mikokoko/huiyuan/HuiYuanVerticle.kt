package com.mikokoko.huiyuan

import com.mikokoko.utils.JoinUtils
import io.vertx.core.AbstractVerticle

class HuiYuanVerticle  : AbstractVerticle() {

    override fun start() {
        val server = vertx.createHttpServer()
        server.requestHandler { req ->
            req.response()
                .putHeader("content-type","text/plain")
                .end(JoinUtils.join("Hello from Vert.x!"))
        }.listen(8080)
    }
}