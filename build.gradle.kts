import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.21"
    application
}

group = "com.tonymacdonald1995"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://m2.dv8tion.net/releases")
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("net.dv8tion:JDA:5.0.0-beta.2")
    implementation("com.theokanning.openai-gpt3-java:service:0.11.0")
    implementation("org.json:json:20220924")
}

tasks.jar {
    archiveFileName.set("Tod.jar")
    manifest {
        attributes["Main-Class"] = "com.tonymacdonald1995.tod.Tod"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    val contents = configurations.runtimeClasspath.get().map {
        if (it.isDirectory)
            it
        else
            zipTree(it)
    } + sourceSets.main.get().output

    from(contents)
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("com.tonymacdonald1995.tod.Tod")
}