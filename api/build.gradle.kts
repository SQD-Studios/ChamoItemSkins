plugins {
    id("maven-publish")
    id("java")
    id("org.jetbrains.dokka")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")

    // Dokka (Better Javadocs)
    dokkaPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:2.2.0")
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