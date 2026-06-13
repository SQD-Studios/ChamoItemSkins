// --- plugin/build.gradle.kts ---
plugins {
    id("com.gradleup.shadow") version "9.4.2"
    id("java")
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

/*repositories {
    mavenCentral()
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven {
        name = "eldonexus"
        url = uri("https://eldonexus.de/repository/maven-public/")
    }
}
 */

dependencies {
    implementation(project(":api"))

    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
    compileOnly("me.clip:placeholderapi:2.11.6")
    implementation("io.github.toxicity188:bettermodel-api:3.1.0")
    compileOnly("net.strokkur.commands:annotations-paper:2.1.1")
    annotationProcessor("net.strokkur.commands:processor-paper:2.1.1")

    implementation("com.zaxxer:HikariCP:6.2.1")
}

tasks.shadowJar {
    configurations = project.configurations.runtimeClasspath.map { setOf(it) }
    archiveClassifier.set("")
    relocate("com.zaxxer.hikari", "net.chamosmp.chamoitemskins.libs.hikari")
    relocate("org.slf4j", "net.chamosmp.chamoitemskins.libs.slf4j")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks {
    runServer {
        downloadPlugins {
            github("toxicity188", "BetterModel", "3.1.0", "bettermodel-3.1.0-paper.jar")
        }

        minecraftVersion("26.1.2")
    }
    runPaper.folia.registerTask()
}