group = "io.github.arcaneplugins"
description = description
version = version

plugins {
    id("java")
    kotlin("jvm") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

apply(plugin = "java")
apply(plugin = "java-library")
apply(plugin = "kotlin")
apply(plugin = "com.github.johnrengelman.shadow")

dependencies {
    implementation(kotlin("stdlib", version = "1.9.22"))
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.22")
    implementation("com.github.Redempt:Crunch:2.0.3")
    implementation("org.bstats:bstats-bukkit:3.0.2")
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("LibsDisguises:LibsDisguises:10.0.42-SNAPSHOT")
    compileOnly("net.essentialsx:EssentialsX:2.20.1")
    compileOnly("dev.folia:folia-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.3")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.1.0-SNAPSHOT")
    compileOnly("de.tr7zw:item-nbt-api-plugin:2.12.2")
    compileOnly("io.github.stumper66:LM_Items:1.3.0")
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://repo.md-5.net/content/groups/public/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.essentialsx.net/releases/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.codemc.org/repository/maven-public")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo1.maven.org/maven2/")
    maven("https://redempt.dev")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    withSourcesJar()
    withJavadocJar()
}

tasks {
    shadowJar {
        archiveBaseName.set("LevelledMobs")
        archiveClassifier.set("")
        dependencies{
            relocate("org.bstats", "io.github.arcaneplugins.levelledmobs.libs.bstats")
            relocate("redempt.crunch", "io.github.arcaneplugins.levelledmobs.libs.crunch")
            relocate("org.jetbrains.annotations", "io.github.arcaneplugins.levelledmobs.libs.jetbrains.annotations")
        }
    }
    compileKotlin {
        kotlinOptions {
            jvmTarget = "17"
        }
    }

    compileJava {
        options.isDeprecation = true
        options.encoding = "UTF-8"

        dependsOn(clean)
    }

    processResources {
        outputs.upToDateWhen { false }
        filesMatching("plugin.yml") {
            expand(mapOf(
                "version" to version,
                "description" to project.findProperty("description")
            ))
        }
    }

    build {
        dependsOn(shadowJar)
    }
}

