plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.0.1"
}

group = "com.github.novotnyr"
version = "0.17-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.8.4")
    implementation("com.auth0:java-jwt:3.2.0")
    implementation("org.ocpsoft.prettytime:prettytime:4.0.2.Final")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.61")

    testImplementation("junit:junit:4.13.2")

    intellijPlatform {
        intellijIdeaCommunity("2022.3.3")
        instrumentationTools()
        pluginVerifier()
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "223"
            untilBuild = "242.*"
        }
        changeNotes = """
            <ul>
            <li>Make compatible with 2024.2</li>
            <li>Require at least 2022.3</li>
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