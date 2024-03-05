plugins {
    id("java")
}

group = "net.flamgop"
version = "1.0.0"

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation("net.minestom:minestom-snapshots:7320437640")
}