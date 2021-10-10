import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

plugins {
    kotlin("jvm") version "1.5.31"
    application
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "me.pengjinfei"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val vertxVersion = "4.1.5"
val junitJupiterVersion = "5.7.0"

val mainVerticleName = "com.mikokoko.MainVerticle"
val launcherClassName = "io.vertx.core.Launcher"

val watchForChange = "src/**/*"
val doOnChange = "${projectDir}/gradlew classes"

dependencies {
    implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
    implementation("io.vertx:vertx-web-client")
    implementation("io.vertx:vertx-web-validation")
    implementation("io.vertx:vertx-web")
    implementation("io.vertx:vertx-mysql-client")
    implementation("io.vertx:vertx-web-sstore-cookie")
    implementation("io.vertx:vertx-web-sstore-redis")
    implementation("io.vertx:vertx-lang-kotlin")
    implementation("io.vertx:vertx-redis-client")
    implementation(kotlin("stdlib-jdk8"))
    testImplementation("io.vertx:vertx-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "1.8"

tasks.withType<ShadowJar> {
    archiveClassifier.set("fat")
    manifest {
        attributes(mapOf("Main-Verticle" to mainVerticleName))
    }
    mergeServiceFiles()
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events = setOf(PASSED, SKIPPED, FAILED)
    }
}

tasks.withType<JavaExec> {
    args = listOf("run", mainVerticleName, "--redeploy=$watchForChange", "--launcher-class=$launcherClassName", "--on-redeploy=$doOnChange")
}

application {
    mainClass.set(launcherClassName)
}