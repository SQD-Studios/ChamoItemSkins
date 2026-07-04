plugins {
    id("com.gradleup.shadow") version "9.4.3"
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("maven-publish")
    id("org.jetbrains.dokka")
}

dependencies {
    implementation(project(":api"))

    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
    compileOnly("me.clip:placeholderapi:2.12.2")
    compileOnly("net.strokkur.commands:annotations-paper:2.1.4")
    annotationProcessor("net.strokkur.commands:processor-paper:2.1.4")
    implementation("com.zaxxer:HikariCP:7.1.0")

    // Convert Options
    compileOnly("de.skyslycer.hmcwraps:api:1.8.2")

    // More plugin support
    compileOnly("com.nexomc:nexo:1.25.0")

    // Dokka (Better Javadocs)
    dokkaPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:2.2.0")
}



tasks {
    // shadoJar configuration
    shadowJar {
        configurations = project.configurations.runtimeClasspath.map { setOf(it) }
        archiveClassifier.set("")
    }

    // We want all jars to produce shadowed ones
    build {
        dependsOn(shadowJar)
    }

    // Enables the ${version} JSON placeholder to plugin.yml
    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filteringCharset = "UTF-8"

        filesMatching("plugin.yml") {
            expand(props)
        }
    }



    // runServer, by my boii JPenilla
    runServer {
        downloadPlugins {
            modrinth("lKEzGugV", "2.12.2")
        }

        minecraftVersion("26.1.2")
    }
    // runFolia
    runPaper.folia.registerTask()
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

dokka {
    pluginsConfiguration.html {
        footerMessage.set("© SQD Studios 2026. ChamoItemSkins is licensed under the Polyform Shield 1.0.0")
    }

}