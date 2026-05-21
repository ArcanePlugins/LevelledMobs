plugins {
    kotlin("jvm") version "2.3.21"
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
    plugins.apply("org.jetbrains.dokka")
}

val javadocJar = tasks.register<Jar>("javadocJar") {
    description = "LevelledMobs javadocs"
    archiveClassifier.set("javadoc")
}

java {
    withJavadocJar()
}