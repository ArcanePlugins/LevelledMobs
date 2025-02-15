import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1

group = "io.github.arcaneplugins"
version = "1.0"
description = "LevelledMobs API"

plugins {
    id("java")
}

dependencies {
    compileOnly(project(":levelledmobs-plugin"))
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
    withJavadocJar()
}