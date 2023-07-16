plugins {
    kotlin("jvm") version "1.6.10"
    id("com.github.johnrengelman.shadow") version "6.0.0"
}

group = "dev.kobalt"
version = "0000.00.00.00.00.00.000"

repositories {
    mavenCentral()
    maven("https://maven.sing-group.org/repository/maven/")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

fun kobalt(module: String, version: String) = "dev.kobalt:$module:$version"
fun ktor(module: String, version: String) = "io.ktor:ktor-$module:$version"
fun exposed(module: String, version: String) = "org.jetbrains.exposed:exposed-$module:$version"
fun general(module: String, version: String) = "$module:$version"
fun kotlinx(module: String, version: String) = "org.jetbrains.kotlinx:kotlinx-$module:$version"
fun kotlinw(module: String, version: String) = "org.jetbrains.kotlin-wrappers:kotlin-$module:$version"

fun DependencyHandler.serialization() {
    implementation(kotlinx("serialization-json", "1.0.0"))
    implementation(kotlinx("serialization-core", "1.0.0"))
}

fun DependencyHandler.commandLineInterface() {
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.3")
}

fun DependencyHandler.standardLibrary() {
    implementation(kotlin("stdlib", "1.6.10"))
}

fun DependencyHandler.logger() {
    implementation(general("org.slf4j:slf4j-simple", "1.7.35"))
}

fun DependencyHandler.htmlDsl() {
    implementation(kotlinx("html-jvm", "0.7.3"))
}

fun DependencyHandler.cssDsl() {
    implementation(kotlinw("css-jvm", "1.0.0-pre.242-kotlin-1.5.30"))
}

dependencies {
    standardLibrary()
    commandLineInterface()
    serialization()
    htmlDsl()
    cssDsl()
    logger()
    implementation("com.github.trilarion:java-vorbis-support:1.2.1")
    // https://mvnrepository.com/artifact/jfugue/jfugue
    implementation("jfugue:jfugue:5.0.9")


}

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        archiveFileName.set("hsp2html.jar")
        mergeServiceFiles()
        minimize()
        manifest {
            attributes("Main-Class" to "dev.kobalt.hsp2html.jvm.MainKt")
        }
    }
}