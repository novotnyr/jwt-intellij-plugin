plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.3"
}

group = "com.github.novotnyr"
version = "0.16-SNAPSHOT"

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
    version = "2022.1"
    type = "IC"
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    patchPluginXml {
        sinceBuild = "221"
        untilBuild = "241.*"
        changeNotes = """
            <ul>
            <li></li>
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