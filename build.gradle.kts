import org.gradle.kotlin.dsl.compileOnly
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin

plugins {
    kotlin("jvm") version libs.versions.kotlin
    alias(libs.plugins.shadow)
}

group = "cc.mewcraft.kommands"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
}

dependencies {
    compileOnly(libs.paper)

    implementation(libs.cloud.core) {
        exclude("org.jetbrains")
    }
    implementation(libs.cloud.paper) {
        exclude("org.jetbrains")
        exclude("net.kyori")
    }
    implementation(libs.cloud.kotlin.coroutines) {
        exclude("org.jetbrains.kotlin")
        exclude("org.jetbrains.kotlinx")
    }
    implementation(libs.cloud.kotlin.extensions) {
        exclude("org.jetbrains.kotlin")
        exclude("org.jetbrains.kotlinx")
    }
}

kotlin {
    jvmToolchain(21)
    sourceSets {
        val main by getting {
            dependencies {
                compileOnly(kotlin("stdlib"))
            }
        }
        val test by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("stdlib"))
            }
        }
    }
}

tasks {
    assemble {
        dependsOn("shadowJar")
    }

    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        relocate("org.incendo.cloud", "cc.mewcraft.kommands.external.cloud") // We don't relocate cloud itself in this example, but you still should
        relocate("io.leangen.geantyref", "cc.mewcraft.kommands.external.geantyref")
        relocate("xyz.jpenilla.reflectionremapper", "cc.mewcraft.kommands.external.reflectionremapper")
        relocate("net.fabricmc.mappingio", "cc.mewcraft.kommands.external.mappingio")
    }
}
