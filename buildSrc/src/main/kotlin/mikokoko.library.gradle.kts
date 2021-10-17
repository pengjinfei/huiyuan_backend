import org.gradle.api.tasks.testing.logging.TestLogEvent

group = "me.pengjinfei"
version = "1.0-SNAPSHOT"

val vertxVersion = "4.1.5"
val junitJupiterVersion = "5.7.0"

plugins {
    id("org.jetbrains.kotlin.jvm")
    `java-library` // <2>
}

repositories {
    mavenLocal()
    maven {
        setUrl("https://maven.aliyun.com/nexus/content/groups/public/")
    }
    mavenCentral()
}

dependencies {
    constraints {
    }
    implementation(kotlin("stdlib-jdk8"))
    implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
    testImplementation("io.vertx:vertx-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    }
}
