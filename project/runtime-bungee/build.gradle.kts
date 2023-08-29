val taboolib_version: String by project

plugins {
    id("io.izzel.taboolib") version "5.37"
}

dependencies {
    compileOnly(project(":project:common"))
    compileOnly("net.md-5:bungeecord-api:1.19-R0.1-SNAPSHOT")
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