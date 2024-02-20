@file:Suppress("PropertyName", "SpellCheckingInspection")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val taboolib_version: String by project

val kotlinVersionNum: String
    get() = project.kotlin.coreLibrariesVersion.replace(".", "")

plugins {
    java
    id("org.jetbrains.kotlin.jvm") version "1.9.10"
    id("com.github.johnrengelman.shadow") version "7.1.2" apply false
}

subprojects {
    apply<JavaPlugin>()
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.github.johnrengelman.shadow")

    repositories {
        mavenLocal()
        mavenCentral()
        maven {
            url = uri("http://ptms.ink:8081/repository/releases/")
            isAllowInsecureProtocol = true
        }
        maven("https://jitpack.io")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://papermc.io/repo/repository/maven-public/")
        maven("https://repo.xenondevs.xyz/releases")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        maven("https://repo.codemc.io/repository/maven-public/")
    }

    dependencies {
        compileOnly(kotlin("stdlib"))
        compileOnly(fileTree("${rootDir.resolve("libs")}"))
        compileOnly("com.google.code.gson:gson:2.8.5")
        compileOnly("com.google.guava:guava:21.0")
        compileOnly("net.kyori:adventure-api:4.16.0")
        implementation("com.eatthepath:fast-uuid:0.2.0")

        compileOnly("io.izzel.taboolib:common:$taboolib_version")
        implementation("io.izzel.taboolib:common-5:$taboolib_version")
        implementation("io.izzel.taboolib:module-chat:$taboolib_version")
        implementation("io.izzel.taboolib:module-configuration:$taboolib_version")
        implementation("io.izzel.taboolib:module-database:$taboolib_version")
        implementation("io.izzel.taboolib:module-kether:$taboolib_version")
        implementation("io.izzel.taboolib:module-lang:$taboolib_version")
        implementation("io.izzel.taboolib:module-metrics:$taboolib_version")
        implementation("io.izzel.taboolib:module-nms:$taboolib_version")
        implementation("io.izzel.taboolib:module-nms-util:$taboolib_version")
        implementation("io.izzel.taboolib:module-ui:$taboolib_version")
        implementation("io.izzel.taboolib:platform-bukkit:$taboolib_version")
        implementation("io.izzel.taboolib:platform-bungee:$taboolib_version")
        implementation("io.izzel.taboolib:platform-velocity:$taboolib_version")
        implementation("io.izzel.taboolib:expansion-alkaid-redis:$taboolib_version")
        implementation("io.izzel.taboolib:expansion-command-helper:$taboolib_version")
        implementation("io.izzel.taboolib:expansion-javascript:$taboolib_version")
        implementation("io.izzel.taboolib:expansion-player-database:$taboolib_version")
    }

    java {
        withSourcesJar()
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = listOf("-Xjvm-default=all", "-Xextended-compiler-checks")
        }
    }
    tasks.withType<ShadowJar> {
        relocate("taboolib", "${rootProject.group}.taboolib")
        relocate("kotlin.", "kotlin${kotlinVersionNum}.") { exclude("kotlin.Metadata") }
        relocate("com.eatthepath.uuid", "${rootProject.group}.library.uuid")
        relocate("@plugin_id@", rootProject.name.toLowerCase())
        relocate("@plugin_name@", rootProject.name)
        relocate("@plugin_version@", rootProject.version.toString())
    }
}

gradle.buildFinished {
    buildDir.deleteRecursively()
}