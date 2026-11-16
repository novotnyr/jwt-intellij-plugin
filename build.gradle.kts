import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.20.1"
}

group = "com.github.novotnyr"
version = "0.25-SNAPSHOT"

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
        intellijIdeaCommunity("2024.3")
        bundledPlugin("com.intellij.modules.json")
        testFramework(TestFrameworkType.Platform)
    }
}

intellijPlatform {
    pluginConfiguration {
        changeNotes = """
            <ul>
                <li>Fix validation of optional <code>exp</code>, <code>nbf</code>, and <code>iat</code> claims</li>
                <li>Support fractional timestamps when editing and re-signing JWTs.</li>
                <li>Keep New JWT dialog secrets separate from the tool window, so Cancel leaves the active JWT unchanged and algorithm switches retain draft secrets.</li>
                <li>Validate RS256 keys before creating a JWT, report errors on the affected key field, and process keys in the background to keep the UI responsive.</li>
                <li>Fix settings change detection and validate configured RSA key files before saving.</li>
                <li>Accept pasted RS256 keys with escaped newlines without changing paste behavior in other editors.</li>
                <li>Refresh claim actions and the Validate button as JWT state changes, and fix stale payload highlighting and error popup placement.</li>
                <li>Format new JWT payload scratch files with the IDE code style even when another editor is selected.</li>
                <li>Require IntelliJ IDEA 2024.3 or later.</li>
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
        register("runIde263") {
            type = IntelliJPlatformType.IntellijIdea
            version = "2026.3"
        }
    }
    testIde {
        register("testIde263") {
            type = IntelliJPlatformType.IntellijIdea
            version = "2026.3"
            testFramework(TestFrameworkType.Platform)
        }
    }
}
