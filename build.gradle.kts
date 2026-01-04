import java.util.Properties

plugins {
    id("java")
    id("com.gradleup.shadow") version "9.0.0-beta12"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
}

group = "org.clockworx.werewolf"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    mavenLocal()
}

dependencies {
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
    
    // Database - Core
    implementation("org.hibernate:hibernate-core:6.6.13.Final") 
    implementation("org.hibernate:hibernate-community-dialects:6.6.13.Final")
    implementation("org.flywaydb:flyway-core:11.7.2")
    implementation("org.flywaydb:flyway-mysql:11.7.2")
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("org.xerial:sqlite-jdbc:3.49.1.0")
    implementation("org.postgresql:postgresql:42.7.5")
    
    // Database - Connection Pools (Shade this)
    implementation("com.zaxxer:HikariCP:6.3.0")
    implementation("org.hibernate.orm:hibernate-hikaricp:6.6.13.Final")

    // Jakarta Persistence API
    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")
    
    // Logging - Make sure we use compatible versions
    implementation("org.jboss.logging:jboss-logging:3.5.3.Final")
    implementation("org.jboss.logging:jboss-logging-annotations:2.2.1.Final")
    // Use Logback for SLF4J implementation compatible with Paper
    implementation("ch.qos.logback:logback-classic:1.5.6")
    
    // Lombok (for boilerplate code reduction)
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")
    
    
    // HTTP Server for resource pack serving
    implementation("org.nanohttpd:nanohttpd:2.3.1")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

// Configure paperweight for Mojang-mapped production output
paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

// Store version at configuration time
val projectVersion = version.toString()

// Version is automatically loaded from gradle.properties by default in recent Gradle versions
// You can access it via project.version or just 'version'
println("Initial project version from properties: $version")

// Set the project version explicitly if needed elsewhere
project.version = version.toString() 

/**
 * Task to increment the patch version in gradle.properties.
 * Refactored to declare inputs/outputs for configuration cache compatibility.
 */
abstract class IncrementPatchVersionTask : DefaultTask() {

    /**
     * The gradle.properties file to modify.
     */
    @get:OutputFile
    abstract val propertiesFile: RegularFileProperty

    /**
     * Executes the version increment logic.
     * Reads the current version from gradle.properties, increments the patch number,
     * and writes the updated version back to the file.
     */
    @TaskAction
    fun execute() {
        val propsFile = propertiesFile.get().asFile
        if (!propsFile.exists()) {
            throw GradleException("gradle.properties file not found at: ${propsFile.path}")
        }

        val props = Properties()
        propsFile.reader(Charsets.UTF_8).use { reader ->
            props.load(reader)
        }

        val currentVersion = props.getProperty("version")
        if (currentVersion == null) {
            throw GradleException("Could not find 'version' property in ${propsFile.path}")
        }
        logger.quiet("Current version from file: $currentVersion")

        val versionRegex = """^(\d+)\.(\d+)\.(\d+)(.*)$""".toRegex()
        val matchResult = versionRegex.find(currentVersion)
            ?: throw GradleException("Version '$currentVersion' does not match expected Major.Minor.Patch format.")

        val (majorStr, minorStr, patchStr, suffix) = matchResult.destructured
        var patch = patchStr.toInt()
        patch++

        val newVersion = "${majorStr}.${minorStr}.$patch$suffix"
        logger.quiet("Incremented version to: $newVersion")

        props.setProperty("version", newVersion)
        propsFile.writer(Charsets.UTF_8).use { writer ->
            props.store(writer, null)
        }
    }
}

// Register the task using the custom task class
val incrementPatchVersion = tasks.register<IncrementPatchVersionTask>("incrementPatchVersion") {
    propertiesFile.set(project.layout.projectDirectory.file("gradle.properties"))
    // Ensure this task only runs if the properties file exists
    onlyIf { propertiesFile.get().asFile.exists() }
    // Ensure this task always runs, as it modifies its output in place
    outputs.upToDateWhen { false }
}

tasks {
    // Configure shadowJar - critical for proper relocation
    shadowJar {
        enableRelocation = false
        archiveClassifier.set("all")
        
        // Add mappings namespace to shadowJar manifest as well
        manifest {
            attributes("paperweight-mappings-namespace" to "mojang")
        }

        // Relocate packages - include all required dependencies
        relocate("com.zaxxer.hikari", "org.clockworx.werewolf.lib.hikari")
        relocate("org.hibernate", "org.clockworx.werewolf.lib.hibernate")
        relocate("org.jboss.logging", "org.clockworx.werewolf.lib.jboss.logging")
        relocate("jakarta.persistence", "org.clockworx.werewolf.lib.jakarta.persistence")
        // SLF4J API is usually provided by the server environment (like Paper), avoid relocating unless necessary
        // relocate("org.slf4j", "org.clockworx.werewolf.lib.slf4j")
        relocate("org.flywaydb", "org.clockworx.werewolf.lib.flywaydb")
        relocate("ch.qos.logback", "org.clockworx.werewolf.lib.logback") // Relocate Logback
        // Relocate the Xerial part of SQLite driver, but NOT the core org.sqlite part
        relocate("org.xerial.sqlite", "org.clockworx.werewolf.lib.xerial.sqlite")
        
        
        // Relocate NanoHTTPD
        relocate("fi.iki.elonen", "org.clockworx.werewolf.lib.nanohttpd")
        
        // IMPORTANT: Specifically exclude the core SQLite package from relocation
        // to prevent breaking native library loading (JNI).
        exclude("org/sqlite/**")

        // Merge service files - critical for service provider loading
        mergeServiceFiles()
    }

    // Configure jar task
    jar {
        manifest {
            attributes(
                "Name" to project.name,
                "Version" to provider { project.version.toString() },
                "Description" to "A modern werewolf plugin for Minecraft",
                "Authors" to "ClockWorX",
                "Main" to "org.clockworx.werewolf.WerewolfPlugin",
                // Tell Paper that this plugin uses Mojang mappings (required for 1.20.5+)
                "paperweight-mappings-namespace" to "mojang"
            )
        }
        from("LICENSE") {
            rename { "${it}_${project.name}" }
        }
        dependsOn("shadowJar")
    }

    clean {
        delete(layout.buildDirectory)
    }
    
    // Configure test task
    test {
        useJUnitPlatform()
    }
    
    // Process resources
    processResources {
        // Process all resource files for version expansion
        // Note: ${project.version} will be replaced with the version value
        filesMatching(listOf("config.yml", "plugin.yml", "paper-plugin.yml", "skins.yml")) {
            // Replace ${project.version} with actual version
            filter { line -> line.replace("\${project.version}", project.version.toString()) }
            expand(
                "version" to project.version
            )
        }
    }
}

// Configure reobfJar to use shadowJar as input and rename output to -paper
// This ensures all resources (plugin.yml, etc.) and dependencies are included
tasks.named("reobfJar").configure {
    val shadowJar = tasks.named("shadowJar")
    val remapJar = this as io.papermc.paperweight.tasks.RemapJar
    // Use the shadowJar output file as input for reobfuscation
    remapJar.inputJar.set(
        shadowJar.flatMap { task -> 
            task.outputs.files.singleFile.let { file ->
                layout.file(providers.provider { file })
            }
        }
    )
    // Rename output to -paper by configuring the output file
    doLast {
        val outputFile = remapJar.outputJar.get().asFile
        val newFile = File(outputFile.parent, outputFile.name.replace("-reobf.jar", "-paper.jar"))
        if (outputFile.exists() && outputFile != newFile) {
            outputFile.renameTo(newFile)
        }
    }
}

// Ensure the production JAR is built with the assemble task
tasks.assemble {
    dependsOn(tasks.reobfJar)
}

// Ensure the 'build' task runs the increment task AFTER finishing
tasks.build {
    finalizedBy(incrementPatchVersion)
}
