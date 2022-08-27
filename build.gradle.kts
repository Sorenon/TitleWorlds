plugins {
    id("fabric-loom") version "0.12-SNAPSHOT"
//    id("io.github.juuxel.loom-quiltflower") version "1.7.1"
    id("maven-publish")
    id("org.quiltmc.quilt-mappings-on-loom") version "4.0.0"
}

base {
    archivesName.set(properties["archives_base_name"].toString())
}

version = "${properties["mod_version"].toString()}+${properties["minecraft_version"].toString()}"
group = properties["maven_group"].toString()

repositories {
    maven {
        name = "ldtteam"
        url = uri("https://ldtteam.jfrog.io/artifactory/parchmentmc-public/")
    }
    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")

        content {
            includeGroup("maven.modrinth")
        }
    }
    maven {
        name = "shedaniel"
        url = uri("https://maven.shedaniel.me/")
    }

    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")
        content {
            includeGroup("maven.modrinth")
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${properties["minecraft_version"].toString()}")
    mappings(loom.layered {
        this.addLayer(quiltMappings.mappings("org.quiltmc:quilt-mappings:${properties["minecraft_version"].toString()}+build.${properties["quilt_mappings"].toString()}:v2"))
        officialMojangMappings()
    })

    modImplementation("net.fabricmc:fabric-loader:${properties["loader_version"].toString()}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${properties["fabric_version"].toString()}")

    modImplementation("me.shedaniel.cloth:cloth-config-fabric:${properties["cloth_config_version"]}") {
        exclude("net.fabricmc.fabric-api")
    }

    modCompileOnlyApi("maven.modrinth:modmenu:${properties["modmenu_version"]}")

//    modCompileOnly("maven.modrinth:sodium:${properties["sodium_version"].toString()}")

    include(implementation("com.electronwill.night-config:core:${properties["night_config_version"].toString()}")!!)
    include(implementation("com.electronwill.night-config:toml:${properties["night_config_version"].toString()}")!!)

    modRuntimeOnly("maven.modrinth:lazydfu:0.1.3") {
        exclude(module = "fabric-loader")
        isTransitive = false
    }
}

loom {
    this.accessWidenerPath.set(file("src/main/resources/titleworlds.accesswidener"))
}

tasks {
    processResources {
        inputs.property("version", project.version)

        filesMatching("fabric.mod.json") {
            expand("version" to project.version)
        }
    }

    withType<JavaCompile> {
        options.release.set(17)
    }

    jar {
        from("LICENSE") {
            rename { "${it}_${project.properties["archivesBaseName"].toString()}" }
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
}

// configure the maven publication
publishing {
    publications {
        create<MavenPublication>("mod") {
            from(components["java"])
        }
    }

    // See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
    repositories {
        // Add repositories to publish to here.
        // Notice: This block does NOT have the same function as the block in the top level.
        // The repositories here will be used for publishing your artifact, not for
        // retrieving dependencies.
    }
}
