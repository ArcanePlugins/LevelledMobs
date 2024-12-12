import org.jetbrains.kotlin.gradle.dsl.JvmTarget

group = "io.github.arcaneplugins"
description = description
version = version

plugins {
    id("java")
    kotlin("jvm") version "2.1.0"
    id("com.gradleup.shadow") version "9.0.0-beta4"
    id("idea")
    id("maven-publish")
}

apply(plugin = "java")
apply(plugin = "java-library")
apply(plugin = "kotlin")
apply(plugin = "com.gradleup.shadow")
apply(plugin = "maven-publish")
apply(plugin = "org.jetbrains.dokka")

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")
    compileOnly("io.papermc.paper:paper-api:1.21.3-R0.1-SNAPSHOT")
    compileOnly("dev.folia:folia-api:1.20.4-R0.1-SNAPSHOT") // https://repo.papermc.io/#browse/browse:maven-public:dev%2Ffolia%2Ffolia-api
    implementation("com.github.Redempt:Crunch:2.0.3") // https://redempt.dev/com/github/Redempt/Crunch
    implementation("org.bstats:bstats-bukkit:3.1.0") // https://mvnrepository.com/artifact/org.bstats/bstats-bukkit
    // implementation("dev.jorel:commandapi-bukkit-shade:9.3.0") // https://github.com/JorelAli/CommandAPI
    // stumper66's fork: https://github.com/stumper66/CommandAPI
    // now found in the lib directory

    compileOnly("LibsDisguises:LibsDisguises:10.0.44-SNAPSHOT") // https://repo.md-5.net/#browse/browse:public:LibsDisguises%2FLibsDisguises
    compileOnly("net.essentialsx:EssentialsX:2.20.1") // https://repo.essentialsx.net/#/releases/net/essentialsx/EssentialsX
    compileOnly("me.clip:placeholderapi:2.11.6") // https://repo.extendedclip.com/content/repositories/placeholderapi/me/clip/placeholderapi/
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.1.0-SNAPSHOT") // https://maven.enginehub.org/repo/com/sk89q/worldguard/worldguard-bukkit/
    compileOnly("de.tr7zw:item-nbt-api-plugin:2.14.0") // https://mvnrepository.com/artifact/de.tr7zw/item-nbt-api-plugin
    compileOnly("io.github.stumper66:LM_Items:1.3.0") // https://mvnrepository.com/artifact/io.github.stumper66/LM_Items

    implementation(fileTree("lib") { include("*.jar") })
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    mavenLocal()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
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
            relocate("kotlin", "io.github.arcaneplugins.levelledmobs.libs.kotlin")
        }
        minimize{}
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = group.toString()
            version = version.toString()
            artifactId = artifactId.toString()
            pom {
                //name.set("…")
                description.set("The Ultimate RPG Mob Levelling Solution")
                url.set("https://github.com/ArcanePlugins/LevelledMobs")
                licenses {
                    license {
                        name.set("GNU")
                        url.set("https://github.com/ArcanePlugins/LevelledMobs/blob/master/LICENSE.md")
                    }
                }
                developers {
                    developer {
                        name.set("Stumper66")
                    }
                    developer {
                        name.set("UltimaOath")
                    }
                    developer {
                        name.set("Lokka30")
                    }
                }
                scm {
                    url.set("https://github.com/ArcanePlugins/LevelledMobs")
                }
            }

            from(components["java"])
        }
    }
}

java {
    withJavadocJar()
}