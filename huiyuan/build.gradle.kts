import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("mikokoko.application")

    id("com.github.johnrengelman.shadow") version "7.0.0"
}

dependencies {
    implementation(project(":common"))
    implementation("io.vertx:vertx-web")
}

val mainVerticleName = "com.mikokoko.huiyuan.HuiYuanVerticle"
val launcherClassName = "io.vertx.core.Launcher"

val watchForChange = "src/**/*"
val doOnChange = "${projectDir}/gradlew classes"

val compileKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "1.8"

tasks.withType<ShadowJar> {
    archiveClassifier.set("fat")
    manifest {
        attributes(mapOf("Main-Verticle" to mainVerticleName))
    }
    mergeServiceFiles()
}

application {
    mainClass.set(launcherClassName)
}
