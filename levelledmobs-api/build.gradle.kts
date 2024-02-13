group = "io.github.arcaneplugins"
version = "1.0"
description = "LevelledMobs API"

plugins {
    id("buildlogic.java-conventions")
    id("java")
}

dependencies {
    //providedCompile(project(":levelledmobs-plugin"))
    compileOnly(project(":levelledmobs-plugin"))
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}