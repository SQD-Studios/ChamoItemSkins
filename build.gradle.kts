plugins {
    id("com.gradleup.shadow") version "9.6.1"
    id("shared")
}

dependencies {
    //dokka(project(":api:"))
    //dokka(project(":plugin:"))

    implementation(project(":plugin"))
    implementation("org.bstats:bstats-bukkit:3.2.1")
}

tasks {
    shadowJar {
        dependsOn(":plugin:shadowJar")

        configurations = project.configurations.runtimeClasspath.map { setOf(it) }
        relocate("org.bstats", project.group.toString())
    }
}