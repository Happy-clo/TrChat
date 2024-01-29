val taboolib_version: String by project

plugins {
    id("io.izzel.taboolib") version "1.56"
}

dependencies {
    compileOnly(project(":project:common"))
    compileOnly("net.md-5:bungeecord-api:1.20-R0.3-SNAPSHOT")
}

taboolib {
    description {
        name(rootProject.name)
        desc("Advanced Minecraft Chat Control")
        links {
            name("homepage").url("https://trchat.trixey.cc/")
        }
        contributors {
            name("Arasple")
            name("ItsFlicker")
        }
    }
    install("common", "platform-bungee")
    options("skip-minimize", "keep-kotlin-module", "skip-taboolib-relocate")
    classifier = null
    version = taboolib_version
}