plugins {
    id("java")
    id("com.gradleup.shadow") version "9.5.1"
    id("maven-publish")
    id("org.jetbrains.dokka") version "2.2.0"
}

allprojects {
    apply(plugin = "java")

    group = "net.chamosmp.chamoitemskins"
    version = "1.0.0"

    repositories {
        mavenCentral {
            name = "Maven Central (HikariCP, BetterModel)"
        }
        maven{
            name = "PaperMC"
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }
        maven {
            name = "Eldonexus"
            url = uri("https://eldonexus.de/repository/maven-public/")
        }
        maven{
            name = "PlaceholderAPI"
            url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        }
        maven("https://repo.hibiscusmc.com/releases/")
        maven("https://repo.nexomc.com/releases")
    }
}


subprojects {
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
            options.compilerArgs.addAll(listOf("-Xlint:-unchecked", "-Xlint:-rawtypes", "-proc:full"))
        }
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

dependencies {
    dokka(project(":api:"))
    dokka(project(":plugin:"))
    dokkaPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:2.2.0")
}

dokka {
    pluginsConfiguration.html {
        footerMessage.set("© SQD Studios 2026. ChamoItemSkins is licensed under the Polyform Shield 1.0.0")
    }

}