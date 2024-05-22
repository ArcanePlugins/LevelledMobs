plugins {
    kotlin("jvm") version "2.0.0"
    id("org.jetbrains.dokka") version "1.9.20"
}

apply(plugin = "kotlin")
apply(plugin = "org.jetbrains.dokka")
apply(plugin = "maven-publish")

repositories {
    mavenCentral()
    maven("https://repo.codemc.io/repository/nms/")
}

dependencies{
    compileOnly("io.github.arcaneplugins:LevelledMobs:$version")
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