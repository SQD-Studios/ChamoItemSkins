plugins {
    id("com.gradleup.shadow") version "9.4.2"
    id("java")
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

dependencies {
    implementation(project(":api"))

    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
    compileOnly("me.clip:placeholderapi:2.11.6")
    implementation("io.github.toxicity188:bettermodel-api:3.1.0")
    implementation("io.github.toxicity188:bettermodel-bukkit-api:3.1.0")
    compileOnly("net.strokkur.commands:annotations-paper:2.1.1")
    annotationProcessor("net.strokkur.commands:processor-paper:2.1.1")

    implementation("com.zaxxer:HikariCP:6.2.1")
    //implementation("org.slf4j:slf4j-jdk14:2.0.16")
}

tasks.shadowJar {
    configurations = project.configurations.runtimeClasspath.map { setOf(it) }
    archiveClassifier.set("")
    relocate("com.zaxxer.hikari", "net.chamosmp.chamoitemskins.libs.hikari")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filteringCharset = "UTF-8"

    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks {
    runServer {
        downloadPlugins {
            github("toxicity188", "BetterModel", "3.1.0", "bettermodel-3.1.0-paper.jar")
            modrinth("lKEzGugV", "2.12.2")
        }

        minecraftVersion("26.1.2")
    }
    runPaper.folia.registerTask()
}

tasks.compileJava {
    options.compilerArgs.addAll(listOf("-Xlint:-unchecked", "-Xlint:-rawtypes", "-proc:full"))
}