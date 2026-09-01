plugins {
    id("com.gradleup.shadow") version "9.6.1"
    id("xyz.jpenilla.run-paper") version "3.1.0"
    id("maven-publish")
    //id("org.jetbrains.dokka") Breaks with runServer
}

dependencies {
    implementation(project(":api"))

    compileOnly("io.papermc.paper:paper-api:26.2.build.+")

    compileOnly("net.strokkur.commands:annotations-paper:2.3.0")
    annotationProcessor("net.strokkur.commands:processor-paper:2.3.0")

    implementation("com.zaxxer:HikariCP:7.1.0")
    implementation("org.bstats:bstats-bukkit:3.2.1")

    // Convert Options
    compileOnly("de.skyslycer.hmcwraps:api:1.8.2")

    // More plugin support
    compileOnly("com.nexomc:nexo:1.27.0")
    compileOnly("me.clip:placeholderapi:2.12.3")

    // Dokka (Better Javadocs)
    //dokkaPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:2.2.0")
}



tasks {
    // shadoJar configuration
    shadowJar {
        configurations = project.configurations.runtimeClasspath.map { setOf(it) }
        archiveClassifier.set("")

        relocate("org.bstats", project.group.toString())
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

        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }


    // runServer, by my boii JPenilla
    runServer {
        downloadPlugins {
            modrinth("lKEzGugV", "2.12.3")
            modrinth("Vebnzrzj", "v5.5.53-bukkit")
        }

        minecraftVersion("26.2")
    }
    // runFolia
    runPaper.folia.registerTask()

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-Xlint:deprecation")
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