plugins {
    id("java")
    id("com.diffplug.spotless") version "6.25.0"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

spotless {
    java {
        removeUnusedImports()
        importOrder("com.wardlordruby", "com.hypixel", "com", "java", "javax")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

group = "com.wardlordruby"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(files("libs/HytaleServer.jar"))
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.build {
    dependsOn(tasks.spotlessApply)
}

tasks.test {
    useJUnitPlatform()
}