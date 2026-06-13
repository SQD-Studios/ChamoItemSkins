// --- build.gradle.kts ---
plugins {
    id("java")
    id("com.gradleup.shadow") version "9.4.2"
}

allprojects {
    group = "net.chamosmp.chamoitemskins"
    version = "1.0.0"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven {
            name = "eldonexus"
            url = uri("https://eldonexus.de/repository/maven-public/")
        }
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        //maven("https://jitpack.io")
    }
}


subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release = 25
    }
}
