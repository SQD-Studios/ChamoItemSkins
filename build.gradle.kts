// --- build.gradle.kts ---
plugins {
    id("java")
    id("com.gradleup.shadow") version "9.4.2"
    id("maven-publish")
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