plugins {
    id("maven-publish")
    kotlin("jvm") version "2.0.20"
}

group = "com.starxg"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.formdev:flatlaf:3.5.1")
    implementation("com.formdev:flatlaf-extras:3.5.1")

}



tasks.test {
    useJUnitPlatform()

    enabled = false
}

kotlin {
    jvmToolchain(8)
}