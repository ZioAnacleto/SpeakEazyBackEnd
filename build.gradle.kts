
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
    application
}

group = "com.zioanacleto"
version = "0.0.1"

application {
    mainClass.set("com.zioanacleto.ApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.h2)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.postgres)
    implementation(libs.cloudinary)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "com.zioanacleto.ApplicationKt"
    }
}

tasks.register<Jar>("fatJar") {
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    manifest {
        attributes["Main-Class"] = "com.zioanacleto.ApplicationKt"
    }
    archiveFileName.set("SpeakEazyBackEnd-all.jar")
}
