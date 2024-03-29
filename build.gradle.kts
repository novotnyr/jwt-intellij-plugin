plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.16.1"
}

group = "com.github.novotnyr"
version = "0.14"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.8.4")
    implementation("com.auth0:java-jwt:3.2.0")
    implementation("org.ocpsoft.prettytime:prettytime:4.0.2.Final")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.61")

    testImplementation("junit:junit:4.13.2")
}

intellij {
    version = "2021.1"
    type = "IC"
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    patchPluginXml {
        sinceBuild = "211"
        untilBuild = "233.*"
        changeNotes = """
            <ul>
            <li>Make compatible with 2023.3</li>
            </ul>
        """.trimIndent()
    }

    buildSearchableOptions {
        enabled = false
    }

    publishPlugin {
        val intellijPublishToken: String by project
        token = intellijPublishToken
    }
}