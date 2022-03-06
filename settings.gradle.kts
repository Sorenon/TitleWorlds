pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        maven {
            name = "Cotton"
            url = uri("https://server.bbkr.space/artifactory/libs-release/")
        }
        maven {
            url = uri("https://maven.quiltmc.org/repository/release")
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
