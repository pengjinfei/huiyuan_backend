package com.mikokoko.merchant

import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Vertx

import io.vertx.core.VertxOptions
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message

import io.vertx.core.spi.cluster.ClusterManager
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager


class MerchantVerticle : AbstractVerticle() {

    override fun start() {
        val mgr: ClusterManager = HazelcastClusterManager()

        val options = VertxOptions().setClusterManager(mgr)

        Vertx.clusteredVertx(options) { res: AsyncResult<Vertx?> ->
            if (res.succeeded()) {
                val vertx = res.result() ?: throw IllegalArgumentException("failed")
                vertx.eventBus().consumer<Buffer>("merchant") { handle(it) }
            } else {
                // failed!
            }
        }
    }

    private fun handle(result: Message<Buffer>) {
        val headers = result.headers()
        val uri = headers.get("uri")
        val buffer = result.body()
        val str = buffer.toString()
        result.reply("hello $uri $str")
    }

}