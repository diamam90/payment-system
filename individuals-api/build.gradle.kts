import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

var properties = mapOf(
    "springCloudFeignVersion" to "4.3.0",
    "springDocVersion" to "2.5.0",
    "logbackEncoderVersion" to "8.0",
    "testContainersKeycloakVersion" to "3.4.0",
    "testContainersJunitVersion" to "1.21.3",
    "personServiceApiVersion" to "1.0.0-SNAPSHOT",
    "wireMockVersion" to "1.0-alpha-13"
)

plugins {
    java
    idea
    `java-library`
    `maven-publish`
    id("org.springframework.boot") version ("3.5.0")
    id("org.openapi.generator") version ("7.14.0")
    id("io.spring.dependency-management") version ("1.1.6")
}

group = "com.example"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("http://localhost:8800/repository/maven-releases")
        isAllowInsecureProtocol = true

    }
    maven {
        url = uri("http://localhost:8800/repository/maven-snapshots")
        isAllowInsecureProtocol = true
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(24))
    }
}

dependencies {
    // SPRING
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign:${properties["springCloudFeignVersion"]}")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // PAYMENT SYSTEM
    implementation("com.example:person-service-api:${properties["personServiceApiVersion"]}")

    // OBSERVABILITY
    implementation("io.micrometer:micrometer-registry-prometheus")

    //  UTIL
//    implementation(platform("org.axonframework:axon-bom:4.12.1"))
//    implementation("org.axonframework:axon-spring-boot-starter")
    implementation("org.aspectj:aspectjweaver")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${properties["springDocVersion"]}")
    implementation("org.projectlombok:lombok")
    implementation("net.logstash.logback:logstash-logback-encoder:${properties["logbackEncoderVersion"]}")
    annotationProcessor("org.projectlombok:lombok")
    implementation("io.netty:netty-resolver-dns-native-macos:4.1.72.Final:osx-aarch_64")

    // TEST
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.github.dasniko:testcontainers-keycloak:${properties["testContainersKeycloakVersion"]}")
    testImplementation("org.testcontainers:junit-jupiter:${properties["testContainersJunitVersion"]}")
    testImplementation("org.wiremock.integrations.testcontainers:wiremock-testcontainers-module:${properties["wireMockVersion"]}")
//    testImplementation("org.axonframework:axon-test")
}


tasks.test {
    useJUnitPlatform()
}

/*
   =================OPEN API GENERATION=================
 */

var inputSpecDir = file("${projectDir}/openapi")
var specifications = inputSpecDir.listFiles { file -> file.extension in listOf("yaml", "yml") } ?: emptyArray()
logger.lifecycle("found ${specifications.size} specifications: " + specifications.joinToString { it.name })

var generateTasks = specifications.map { spec ->
    val specName = spec.nameWithoutExtension
    val taskName = "generate" + specName.split(Regex("[^a-zA-Z0-9]"))
        .filter { it.isNotBlank() }
        .joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }

    tasks.register<GenerateTask>(taskName) {
        generatorName.set("spring")
        inputSpec.set(spec.absolutePath)
        outputDir.set("${layout.buildDirectory.get().asFile}/generated")
        val base = "com.example.${specName.substringBefore("-").lowercase()}"
        importMappings.set(mapOf("ZonedDateTime" to "java.time.ZonedDateTime"))
        typeMappings.set(mapOf("DateTime" to "ZonedDateTime"))
        configOptions.set(
            mapOf(
                "library" to "spring-boot",
                "skipDefaultInterface" to "true",
                "useBeanValidation" to "true",
                "openApiNullable" to "false",
                "useJakartaEe" to "true",
                "useTags" to "true",
                "apiPackage" to "$base.api",
                "modelPackage" to "$base.dto",
                "configPackage" to "$base.config",
                "reactive" to "true",
                "interfaceOnly" to "true",
            )
        )
        doFirst {
            logger.lifecycle("$taskName starting generation from ${spec.name}")
        }
    }
}

tasks.register("generateAllOpenApi") {
    dependsOn(generateTasks)
}

tasks.compileJava {
    dependsOn(tasks.named("generateAllOpenApi"))
}

sourceSets {
    main {
        java {
            srcDir("${layout.buildDirectory.asFile.get()}/generated/src/main/java")
        }
    }
}
