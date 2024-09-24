plugins {
    id("maven-publish")
    kotlin("jvm") version "2.0.20"
}

group = "com.starxg"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(8)
}

java {
    withSourcesJar()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.formdev:flatlaf:3.5.1")
    implementation("com.formdev:flatlaf-extras:3.5.1")

}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
            from(components["java"])
        }
    }
}

tasks.test {
    useJUnitPlatform()
    enabled = false
}
