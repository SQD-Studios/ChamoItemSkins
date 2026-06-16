plugins {
    id("com.gradleup.shadow") version "9.4.2"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

dependencies {
    implementation(project(":api"))

    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("net.strokkur.commands:annotations-paper:2.1.1")
    annotationProcessor("net.strokkur.commands:processor-paper:2.1.1")
    implementation("com.zaxxer:HikariCP:6.2.1")
    implementation("io.github.toxicity188:bettermodel-api:3.1.0")
    implementation("io.github.toxicity188:bettermodel-bukkit-api:3.1.0")
}



tasks {
    // shadoJar configuration
    shadowJar {
        configurations = project.configurations.runtimeClasspath.map { setOf(it) }
        archiveClassifier.set("")
        relocate("com.zaxxer.hikari", "net.chamosmp.chamoitemskins.libs.hikari")
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
            github("toxicity188", "BetterModel", "3.1.0", "bettermodel-3.1.0-paper.jar")
            modrinth("lKEzGugV", "2.12.2")
        }

        minecraftVersion("26.1.2")
    }
    // runFolia
    runPaper.folia.registerTask()
}