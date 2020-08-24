plugins {
    kotlin("jvm") version "1.3.72"
    kotlin("plugin.serialization") version "1.3.72"
}

repositories {

}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("stdlib", "1.2.31"))
    testImplementation("junit:junit:4.12")
}


buildscript {
    repositories { jcenter() }

    dependencies {
        val kotlinVersion = "1.3.72"
        classpath(kotlin("gradle-plugin", version = kotlinVersion))
        classpath(kotlin("serialization", version = kotlinVersion))
    }
}