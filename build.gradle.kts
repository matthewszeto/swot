plugins {
    kotlin("jvm") version "2.0.21"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

kotlin {
    jvmToolchain(21)
}

sourceSets {
    named("main") {
        kotlin.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("lib/domains"))
    }
    named("test") {
        kotlin.setSrcDirs(listOf("test"))
    }
}

tasks.test {
    useJUnit()
}
