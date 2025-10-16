plugins {
    `java-library`
}

group = "Jobs"
version = "5.2.6.4"
description = "Jobs"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()

    // momirealms
    maven("https://repo.momirealms.net/releases/")

    // CodeMC
    maven("https://repo.codemc.org/repository/maven-public/")

    // MythicMobs - Lumine
    maven("https://mvn.lumine.io/repository/maven-public/")

    // WorldGuard & WorldEdit
    maven("https://maven.enginehub.org/repo/")

    // JitPack
    maven("https://jitpack.io")

    // NeetGames
    maven("https://nexus.neetgames.com/repository/maven-releases/")

    // Spigot
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")

    // PlaceholderAPI
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")

    // BG Software
    maven("https://repo.bg-software.com/repository/api/")
}

configurations.all {
    resolutionStrategy {
        // Force specific versions to resolve conflicts
        force("com.google.guava:guava:33.4.8-jre")
        force("com.google.code.gson:gson:2.11.0")
        force("org.apache.logging.log4j:log4j-bom:2.24.1")
        force("org.apache.logging.log4j:log4j-api:2.24.1")
        force("org.apache.logging.log4j:log4j-core:2.24.1")
    }
    // Exclude strict version metadata
    resolutionStrategy.eachDependency {
        if (requested.group == "org.apache.logging.log4j" && requested.name == "log4j-bom") {
            useVersion("2.24.1")
            because("Resolve strict version conflicts")
        }
        if (requested.group == "com.google.guava" && requested.name == "guava") {
            useVersion("33.4.8-jre")
            because("Resolve strict version conflicts")
        }
        if (requested.group == "com.google.code.gson" && requested.name == "gson") {
            useVersion("2.11.0")
            because("Resolve strict version conflicts")
        }
    }
}

dependencies {
    // Spigot API
    compileOnly("org.spigotmc:spigot-api:1.21.10-R0.1-SNAPSHOT")

    // Mojang Authlib
    compileOnly("com.mojang:authlib:1.5.21")

    // McMMO
    compileOnly("com.gmail.nossr50.mcMMO:mcMMO:2.2.004") {
        isTransitive = false
    }

    // Vault
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        isTransitive = false
    }

    // MythicMobs
    compileOnly("io.lumine:Mythic-Dist:5.6.1") {
        isTransitive = false
    }

    // WorldGuard
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.12")

    // WorldEdit
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.10")
    compileOnly("com.sk89q.worldedit:worldedit-core:7.3.10")

    // PlaceholderAPI
    compileOnly("com.github.placeholderapi:placeholderapi:2.11.6") {
        exclude(group = "me.rayzr522", module = "jsonmessage")
        exclude(group = "org.jetbrains", module = "annotations")
    }

    // CMILib
    compileOnly("com.github.Zrips:CMILib:1.5.6.3")

    // WildStacker
    compileOnly("com.bgsoftware:WildStackerAPI:3.8.0")

    // StackMob
    compileOnly("uk.antiperson.stackmob:StackMob:5.8.2")

    // MyPet (local lib)
    compileOnly(files("libs/mypet-3.12.jar"))

    // PyroFishingPro (local lib)
    compileOnly(files("libs/PyroFishingPro-4.9.1.jar"))

    // CustomFishing
    compileOnly("net.momirealms:custom-fishing:2.3.4")
}

tasks {
    withType<JavaCompile> {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()
        filesMatching(listOf("plugin.yml", "*.properties")) {
            expand(project.properties)
        }
    }

    jar {
        archiveBaseName.set("Jobs")
        archiveVersion.set(project.version.toString())
    }

    defaultTasks("clean", "build")
}
