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
    implementation("com.zaxxer:HikariCP:6.2.1")
    implementation("com.impossibl.pgjdbc-ng:pgjdbc-ng:0.8.9")
    implementation("ch.qos.logback:logback-classic:1.5.12")
    implementation("io.github.cdimascio:dotenv-java:3.0.2")

    implementation("net.dv8tion:JDA:5.2.1") {
        exclude("opus-java")
    }
}

application {
    mainClass.set("org.maplestar.syrup.Main")
}
