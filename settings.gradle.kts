plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("1.0.0")
}

gradle.extra["projectName"] = "Werewolf"
rootProject.name = gradle.extra["projectName"].toString().lowercase()

// Include the shared Clockworx data library as a composite build
includeBuild("../clockworx-data") {
    dependencySubstitution {
        substitute(module("org.clockworx:clockworx-data")).using(project(":"))
    }
}
