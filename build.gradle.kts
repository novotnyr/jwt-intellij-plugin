plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.13.1"
}

group = "com.github.novotnyr"
version = "0.22-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation("org.jspecify:jspecify:1.0.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.8.4")
    implementation("com.auth0:java-jwt:4.5.1")
    implementation("org.ocpsoft.prettytime:prettytime:4.0.2.Final")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.61")

    testImplementation("junit:junit:4.13.2")

    intellijPlatform {
        intellijIdeaCommunity("2023.1.7")
        pluginVerifier()
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "231"
        }
        changeNotes = """
            <ul>
            </ul>
        """.trimIndent()
    }
    publishing {
        val intellijPublishToken: String by project
        token = intellijPublishToken
    }
    pluginVerification {
        ides {
            recommended()
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}