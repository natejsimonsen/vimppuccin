import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Pure-JVM module: packet parsing, DNS handling and blocklist matching.
// Kept free of Android APIs so it can be unit-tested on any JVM.
plugins {
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    testImplementation(kotlin("test-junit"))
}
