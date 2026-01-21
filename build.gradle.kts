plugins {
    id("java")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
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

tasks.test {
    useJUnitPlatform()
}