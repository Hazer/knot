plugins {
    base
    kotlin("jvm") version Deps.Version.kotlin apply false
}

allprojects {
    group = "de.halfbit"
    version = "1.0-rc2"

    repositories {
        mavenCentral()
        jcenter()
    }
}
