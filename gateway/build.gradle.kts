
plugins {
    id("mikokoko.library")
}

dependencies {
    implementation(project(":common"))
    implementation("io.vertx:vertx-web")
}

val compileKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "1.8"