plugins {
    kotlin("jvm") version "2.1.20"
    id("org.jetbrains.dokka") version "2.0.0"
}

repositories {
    mavenCentral()
    maven("https://mvnrepository.com/")
}

dependencies{
    compileOnly("io.github.arcaneplugins:levelledmobs-plugin:$version")
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