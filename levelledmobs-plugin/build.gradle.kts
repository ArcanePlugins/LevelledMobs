group = "io.github.arcaneplugins"
description = description
version = version

plugins {
    id("java")
    kotlin("jvm") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("idea")
}

apply(plugin = "java")
apply(plugin = "java-library")
apply(plugin = "kotlin")
apply(plugin = "com.github.johnrengelman.shadow")

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

dependencies {
    implementation(kotlin("stdlib", version = "1.9.22"))
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.22")
    implementation("com.github.Redempt:Crunch:2.0.3") // https://redempt.dev/com/github/Redempt/Crunch
    implementation("org.bstats:bstats-bukkit:3.0.2") // https://mvnrepository.com/artifact/org.bstats/bstats-bukkit
    //implementation("dev.jorel:commandapi-bukkit-shade:9.3.0") // https://github.com/JorelAli/CommandAPI
    implementation("nomaven:CommandAPI:9.3.0-mod") // https://github.com/JorelAli/CommandAPI
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("LibsDisguises:LibsDisguises:10.0.42-SNAPSHOT") // https://repo.md-5.net/#browse/browse:public:LibsDisguises%2FLibsDisguises
    compileOnly("net.essentialsx:EssentialsX:2.20.1") // https://repo.essentialsx.net/#/releases/net/essentialsx/EssentialsX
    compileOnly("dev.folia:folia-api:1.20.2-R0.1-SNAPSHOT") // https://repo.papermc.io/#browse/browse:maven-public:dev%2Ffolia%2Ffolia-api
    compileOnly("me.clip:placeholderapi:2.11.5") // https://repo.extendedclip.com/content/repositories/placeholderapi/me/clip/placeholderapi/
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.1.0-SNAPSHOT") // https://maven.enginehub.org/repo/com/sk89q/worldguard/worldguard-bukkit/
    compileOnly("de.tr7zw:item-nbt-api-plugin:2.12.2") // https://mvnrepository.com/artifact/de.tr7zw/item-nbt-api-plugin
    compileOnly("io.github.stumper66:LM_Items:1.3.0") // https://mvnrepository.com/artifact/io.github.stumper66/LM_Items
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    mavenLocal()
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
        archiveFileName.set("LevelledMobs-$version.jar")
        dependencies{
            relocate("dev.jorel.commandapi", "io.github.arcaneplugins.levelledmobs.libs.commandapi")
            relocate("org.bstats", "io.github.arcaneplugins.levelledmobs.libs.bstats")
            relocate("redempt.crunch", "io.github.arcaneplugins.levelledmobs.libs.crunch")
            relocate("org.jetbrains.annotations", "io.github.arcaneplugins.levelledmobs.libs.jetbrains.annotations")
        }
        // do not use minimize, it breaks reflection use
        //minimize()
    }

    jar.configure {
        actions.clear()
        dependsOn(shadowJar)
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

