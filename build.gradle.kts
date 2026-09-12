plugins {
    java
}

group = "com.dksourov"
version = "0.1.0-26.2"

repositories {
    maven { name = "papermc"; url = uri("https://repo.papermc.io/repository/maven-public/") }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

jar {
    archiveBaseName.set("Player2NPC-Paper-26.2")
}
