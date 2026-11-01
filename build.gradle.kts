import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "com.github.novotnyr"
version = "0.24-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation("org.jspecify:jspecify:1.0.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.21.2")
    implementation("com.auth0:java-jwt:4.5.1")
    implementation("org.ocpsoft.prettytime:prettytime:4.0.2.Final")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.83")

    testImplementation("junit:junit:4.13.2")

    intellijPlatform {
        intellijIdeaCommunity("2024.2")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "242"
        }
        changeNotes = """
            <ul>
                <li></li>
            </ul>
        """.trimIndent()
    }
    publishing {
        token = project.property("intellijPublishToken") as String
    }
    buildSearchableOptions = false
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

intellijPlatformTesting {
    runIde {
        register("runIde261") {
            type = IntelliJPlatformType.IntellijIdea
            version = "2026.1"
        }
    }
}
