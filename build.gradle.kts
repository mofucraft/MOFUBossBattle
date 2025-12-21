plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "net.mofucraft"
version = "0.0.2"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://mvn.lumine.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    // Paper API
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")

    // MythicMobs API
    compileOnly("io.lumine:Mythic-Dist:5.6.1")

    // PlaceholderAPI
    compileOnly("me.clip:placeholderapi:2.11.6")

    // HikariCP for database connection pooling
    implementation("com.zaxxer:HikariCP:5.1.0")

    // MySQL Connector
    implementation("com.mysql:mysql-connector-j:8.2.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        // Relocation disabled due to ASM compatibility with Java 21
        // relocate("com.zaxxer.hikari", "net.mofucraft.bossbattle.lib.hikari")
        // relocate("com.mysql", "net.mofucraft.bossbattle.lib.mysql")
    }

    build {
        dependsOn(shadowJar)
    }

    processResources {
        filesMatching("plugin.yml") {
            expand("version" to version)
        }
    }
}
