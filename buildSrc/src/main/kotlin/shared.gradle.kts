plugins {
    id("java")
    id("maven-publish")
    //id("org.jetbrains.dokka") Breaks with runServer
}

group = "net.chamosmp.chamoitemskins"
version = "1.1.1"

repositories {
    mavenCentral {
        name = "Maven Central (HikariCP, BetterModel)"
    }
    maven {
        name = "PaperMC"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "Eldonexus"
        url = uri("https://eldonexus.de/repository/maven-public/")
    }
    maven {
        name = "PlaceholderAPI"
        url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }
    maven("https://repo.hibiscusmc.com/releases/")
    maven("https://repo.nexomc.com/releases")
    maven {
        name = "chamosmpRepoReleases"
        url = uri("https://maven.chamosmp.net/releases")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "ChamoSMP-Releases"
            url = uri("https://maven.chamosmp.net/releases")
            credentials {
                username = System.getenv("REPOSILITE_USER")
                password = System.getenv("REPOSILITE_TOKEN")
            }
        }
        maven {
            name = "ChamoSMP-Snapshots"
            url = uri("https://maven.chamosmp.net/snapshots")
            credentials {
                username = System.getenv("REPOSILITE_USER")
                password = System.getenv("REPOSILITE_TOKEN")
            }
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }

}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release = 25
    }

    // Display all deprecation warnings
    compileJava {
        options.compilerArgs.addAll(listOf("-proc:full", "-Xlint:deprecation"))
    }
}

dependencies {
    // Dokka (Better Javadocs)
    //dokkaPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:2.2.0")

    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
}

/*
dokka {
    pluginsConfiguration.html {
        customAssets.from("../assets/ChamoItemSkins.png", "../assets/logo-icon.svg")
        customStyleSheets.from(
            "../assets/dokka/style.css",
            "../assets/dokka/prism.css",
            "../assets/dokka/main.css",
            "../assets/dokka/logo-styles.css"
        )
        footerMessage.set("© SQD Studios 2026. ChamoItemSkins is licensed under the Polyform Shield 1.0.0")
    }
}

 */