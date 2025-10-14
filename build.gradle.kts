plugins {
    id("java")
    id("application")
}

group = "org.maplestar"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("com.impossibl.pgjdbc-ng:pgjdbc-ng:0.8.9")
    implementation("ch.qos.logback:logback-classic:1.5.19")
    implementation("io.github.cdimascio:dotenv-java:3.2.0")

    implementation("net.dv8tion:JDA:6.0.0") {
        exclude("opus-java")
    }
}

application {
    mainClass.set("org.maplestar.syrup.Main")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }
}
