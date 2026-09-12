plugins {
    id("com.gradleup.shadow") version "9.6.1"
    id("shared")
}

dependencies {
    //dokka(project(":api:"))
    //dokka(project(":plugin:"))

    implementation(project(":plugin"))
}