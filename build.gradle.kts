plugins {
    id("java")
    id ("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "net.thiccaxe.gradient"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.kyori:adventure-api:4.16.0")
    implementation("net.kyori:adventure-text-serializer-ansi:4.16.0")
}


