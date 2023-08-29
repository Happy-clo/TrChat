val taboolib_version: String by project

plugins {
    id("io.izzel.taboolib") version "5.37"
}

dependencies {
    compileOnly(project(":project:common"))
    compileOnly("com.velocitypowered:velocity-api:3.1.1")
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
    install("common", "platform-velocity")
    options("skip-minimize", "keep-kotlin-module", "skip-taboolib-relocate")
    classifier = null
    version = taboolib_version
}