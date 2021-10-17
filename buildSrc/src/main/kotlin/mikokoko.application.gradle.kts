import gradle.kotlin.dsl.accessors._ba99aed39ca7d0b39b88b6122195cc59.implementation

plugins {
    id("mikokoko.library") // <1>

    application // <2>
}

dependencies {
    implementation("io.vertx:vertx-core")
}
