plugins {
    kotlin("multiplatform") version "1.9.23"
    id("maven-publish")
}

group = "com.ilogos.shared"
version = "0.0.1"

val jjwtVersion = "0.11.5"
val jsJwtVersion = "9.0.2"

repositories {
    mavenCentral()
}

kotlin {
    jvm()
    js(IR) {
        nodejs()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
                runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
                runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")
                implementation("org.springframework.security:spring-security-oauth2-jose:6.5.0")
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(npm("jsonwebtoken", jsJwtVersion))
            }
        }
    }
}

publishing {
    repositories {
        mavenLocal()
    }
}
