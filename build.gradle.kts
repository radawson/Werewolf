import java.util.Properties

plugins {
    id("java")
    id("com.gradleup.shadow") version "9.5.1"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

group = "org.clockworx.werewolf"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    mavenLocal()
}

dependencies {
    paperweight.paperDevBundle("26.1.2.build.74-stable")
    
    // Shared Clockworx data layer (Hibernate + Flyway + HikariCP + JDBC drivers)
    // Provided via composite build from ../clockworx-data (see settings.gradle.kts)
    implementation("org.clockworx:clockworx-data:0.1.0-SNAPSHOT")

    // Use Logback for SLF4J implementation compatible with Paper
    implementation("ch.qos.logback:logback-classic:1.5.37")
    
    // Lombok (for boilerplate code reduction)
    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")
    
    // bStats Metrics
    implementation("org.bstats:bstats-bukkit:3.1.0")
    
    // HTTP Server for resource pack serving
    implementation("org.nanohttpd:nanohttpd:2.3.1")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

// Configure paperweight for Mojang-mapped production output

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
        archiveClassifier.set("all")
        // Avoid "Could not add file to ZIP" on duplicate entries (JDK 25 / paperweight 26.x)
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

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
        
        // Relocate bStats to avoid conflicts with other plugins
        relocate("org.bstats", "${project.group}.lib.bstats")
        
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

// Paper 1.20.5+ runs on Mojang mappings natively; no reobfuscation step is needed.
// The Mojang-mapped shadowJar (-all.jar) is the production artifact.
tasks.assemble {
    dependsOn(tasks.shadowJar)
}

// Ensure the 'build' task runs the increment task AFTER finishing
tasks.build {
    finalizedBy(incrementPatchVersion)
}
