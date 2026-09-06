plugins {
    id("com.gradleup.shadow") version "9.6.1"
    id("xyz.jpenilla.run-paper") version "3.1.0"
    id("shared")
}

dependencies {
    implementation(project(":api"))

    implementation("net.chamosmp.sqdlib:sqd-lib:1.1.3")

    compileOnly("net.strokkur.commands:annotations-paper:2.3.0")
    annotationProcessor("net.strokkur.commands:processor-paper:2.3.0")

    compileOnly("com.zaxxer:HikariCP:7.1.0")
    implementation("org.bstats:bstats-bukkit:3.2.1")

    // Convert Options
    compileOnly("de.skyslycer.hmcwraps:api:1.8.2")

    // More plugin support
    compileOnly("com.nexomc:nexo:1.27.0")
    compileOnly("me.clip:placeholderapi:2.12.3")

}

tasks {
    // shadoJar configuration
    shadowJar {
        configurations = project.configurations.runtimeClasspath.map { setOf(it) }
        archiveClassifier.set("")

        relocate("org.bstats", project.group.toString())
        relocate("net.chamosmp.sqdlib", "net.chamosmp.chamoitemskins.libs.sqdlib")
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