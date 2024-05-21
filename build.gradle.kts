plugins {
    kotlin("jvm") version "1.9.23"
    id("org.jetbrains.dokka") version "1.9.20"
}

apply(plugin = "kotlin")
apply(plugin = "org.jetbrains.dokka")
apply(plugin = "maven-publish")

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("io.github.arcaneplugins:LevelledMobs:4.0.0 b6")
}

subprojects {
    apply(plugin = "org.jetbrains.dokka")
}

val javadocJar = tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
}

java {
    withJavadocJar()
}