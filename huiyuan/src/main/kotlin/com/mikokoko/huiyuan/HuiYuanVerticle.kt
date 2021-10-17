package com.mikokoko.huiyuan

import com.mikokoko.common.gateway.Http2MsgHandler
import com.mikokoko.common.gateway.YiTongBodyHandler
import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.spi.cluster.ClusterManager
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager

class HuiYuanVerticle  : AbstractVerticle() {

    override fun start() {
        val mgr: ClusterManager = HazelcastClusterManager()

        val options = VertxOptions().setClusterManager(mgr)

        Vertx.clusteredVertx(options) { res: AsyncResult<Vertx?> ->
            if (res.succeeded()) {
                val vertx = res.result() ?: throw IllegalArgumentException("failed")
                val server = vertx.createHttpServer()
                val http2MsgHandler = Http2MsgHandler()
                val router = Router.router(vertx)

                val yiTongBodyHandler = YiTongBodyHandler()
                yiTongBodyHandler.setBodyLimit(10240)
                router.route().handler(yiTongBodyHandler);
                router.route().handler(http2MsgHandler)
                server.requestHandler(router).listen(8080)
            } else {
                // failed!
            }
        }
    }
}