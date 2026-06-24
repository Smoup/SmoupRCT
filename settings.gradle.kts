pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
    // Версия централизована здесь; в build.gradle.kts плагин применяется без версии
    // (тот же приём, что и для loom-back-compat).
    plugins {
        id("me.modmuss50.mod-publish-plugin") version "2.0.1"
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.6"
    id("dev.kikugie.loom-back-compat") version "0.3"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        // По одной версии на «семейство» API. Полный список: https://modmuss50.me/fabric.html
        versions("1.20.6", "1.21.1", "1.21.4", "1.21.8", "1.21.11")
        // Ключ узла "26.1" -> фактическая версия MC "26.1.2".
        version("26.1", "26.1.2")
        vcsVersion = "1.21.11"
    }
}

rootProject.name = "SmoupRCT"
