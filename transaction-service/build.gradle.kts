import org.openapitools.generator.gradle.plugin.tasks.GenerateTask
import org.gradle.api.publish.maven.MavenPublication

val versions = mapOf(
    "mapstructVersion" to "1.5.5.Final",
    "springdocOpenapiStarterWebmvcUiVersion" to "2.5.0",
    "javaxAnnotationApiVersion" to "1.3.2",
    "javaxValidationApiVersion" to "2.0.0.Final",
    "comGoogleCodeFindbugs" to "3.0.2",
    "springCloudStarterOpenfeign" to "4.1.1",
    "javaxServletApiVersion" to "2.5",
    "logbackClassicVersion" to "1.5.18",
    "comGoogleCodeFindbugs" to "3.0.2",
    "springCloudStarterOpenfeign" to "4.1.1",
    "hibernateEnversVersion" to "6.4.4.Final",
    "testContainersVersion" to "1.19.3",
    "junitJupiterVersion" to "5.10.0",
    "feignMicrometerVersion" to "13.6",
    "shardingSphereVersion" to "5.5.2",
    "hibernateJpamodelgenVersion" to "6.1.7.Final",
    "testContainersKeycloakVersion" to "3.4.0",
    "logbackEncoderVersion" to "8.0"
)

plugins {
    idea
    java
    id("org.springframework.boot") version "3.5.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("maven-publish")
    id("org.openapi.generator") version "7.13.0"
}

group = "com.example"
version = "1.0.0-SNAPSHOT"
description = "Transaction domain service for study project"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.0.0")
        mavenBom("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom:2.15.0")
    }
}

configurations.all { resolutionStrategy.cacheChangingModulesFor(0, "seconds") }

dependencies {
    // SPRING
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${versions["springdocOpenapiStarterWebmvcUiVersion"]}")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign:${versions["springCloudStarterOpenfeign"]}")
    implementation("org.springframework.kafka:spring-kafka")

    // OBSERVABILITY
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.github.openfeign:feign-micrometer:${versions["feignMicrometerVersion"]}")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("io.micrometer:micrometer-observation")
    implementation("io.micrometer:micrometer-tracing")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    implementation("io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter")
    implementation("ch.qos.logback:logback-classic:${versions["logbackClassicVersion"]}")
    implementation("net.logstash.logback:logstash-logback-encoder:${versions["logbackEncoderVersion"]}")

    // PERSISTENCE
    implementation("org.hibernate.orm:hibernate-envers:${versions["hibernateEnversVersion"]}")
    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.apache.shardingsphere:shardingsphere-jdbc:${versions["shardingSphereVersion"]}")
    annotationProcessor("org.hibernate:hibernate-jpamodelgen:${versions["hibernateJpamodelgenVersion"]}")

    // HELPERS
    compileOnly("org.projectlombok:lombok")
    compileOnly("org.mapstruct:mapstruct:${versions["mapstructVersion"]}")
    compileOnly("com.google.code.findbugs:jsr305:${versions["comGoogleCodeFindbugs"]}")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.mapstruct:mapstruct-processor:${versions["mapstructVersion"]}")
    implementation("javax.validation:validation-api:${versions["javaxValidationApiVersion"]}")
    implementation("javax.annotation:javax.annotation-api:${versions["javaxAnnotationApiVersion"]}")

    // TEST
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testImplementation("org.junit.jupiter:junit-jupiter:${versions["junitJupiterVersion"]}")
    testImplementation("org.testcontainers:testcontainers:${versions["testContainersVersion"]}")
    testImplementation("com.github.dasniko:testcontainers-keycloak:${versions["testContainersKeycloakVersion"]}")
    testImplementation("org.testcontainers:postgresql:${versions["testContainersVersion"]}")
    testImplementation("org.testcontainers:junit-jupiter:${versions["testContainersVersion"]}")
    testImplementation("org.testcontainers:kafka:${versions["testContainersVersion"]}")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(24))
    }
}

tasks.test {
    useJUnitPlatform()
}


/*
   =================OPEN API GENERATION=================
*/

val inputSpecDir = file("$projectDir/openapi")
val specifications = inputSpecDir.listFiles { file -> file.extension in listOf("yaml", "yml") } ?: emptyArray()
logger.lifecycle("found ${specifications.size} specifications: " + specifications.joinToString { it.name })

var generatedTasks = specifications.map { spec ->
    val specName = spec.nameWithoutExtension
    val taskName = buildOpenApiTaskName(specName)

    tasks.register<GenerateTask>(taskName) {
        generatorName.set("spring")
        inputSpec.set(spec.absolutePath)
        outputDir.set(layout.buildDirectory.dir("generated-sources/openapi/$specName").get().asFile.absolutePath)
        importMappings.set(mapOf("ZonedDateTime" to "java.time.ZonedDateTime"))
        typeMappings.set(mapOf("DateTime" to "ZonedDateTime"))
        val base = "com.example.${specName.substringBefore("-").lowercase()}"
        configOptions.set(
            mapOf(
                "library" to "spring-cloud",
                "skipDefaultInterface" to "true",
                "useBeanValidation" to "true",
                "openApiNullable" to "false",
                "useJakartaEe" to "true",
                "useFeignClientUrl" to "true",
                "useTags" to "true",
                "apiPackage" to "$base.api",
                "modelPackage" to "$base.dto",
                "configPackage" to "$base.config"
            )
        )
        doFirst {
            logger.lifecycle("$taskName starting generation from ${spec.name}")
        }
    }
}

fun buildJarTaskName(nameWithoutExtension: String): String {
    return buildTaskName("jar", nameWithoutExtension);
}

fun buildOpenApiTaskName(nameWithoutExtension: String): String {
    return buildTaskName("generate", nameWithoutExtension);
}

fun buildCompileTaskName(nameWithoutExtension: String): String {
    return buildTaskName("compile", nameWithoutExtension)
}

fun buildPublishTaskName(nameWithoutExtension: String): String {
    return buildTaskName("", nameWithoutExtension)
}

fun buildTaskName(taskPrefix: String, nameWithoutExtension: String): String {
    val name = nameWithoutExtension
        .split(Regex("[^a-zA-Z0-9]"))
        .filter { it.isNotBlank() }
        .joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
    return "$taskPrefix$name"
}

sourceSets {
    specifications.forEach { spec ->
        named("main") {
            java.srcDirs(layout.buildDirectory.dir("generated-sources/openapi/${spec.nameWithoutExtension}/src/main/java"))
        }
    }
}

tasks.named("build") {
    dependsOn(jars)
}

val jars = specifications.map { spec ->
    val specName = spec.nameWithoutExtension

    val srcSet = sourceSets.create(specName) {
        java.srcDirs(layout.buildDirectory.dir("generated-sources/openapi/${specName}/src/main/java"))
        compileClasspath += sourceSets["main"].compileClasspath
        runtimeClasspath += sourceSets["main"].runtimeClasspath
    }

    val compileTaskName = buildCompileTaskName(specName)
    tasks.register<JavaCompile>(compileTaskName) {
        source(srcSet.java)
        classpath = srcSet.compileClasspath
        destinationDirectory.set(layout.buildDirectory.dir("classes/${specName}"))
        dependsOn(buildOpenApiTaskName(specName))
        doFirst {
            logger.lifecycle("Compiling classes from ${srcSet.java}")
        }
    }

    val jarTaskName = buildJarTaskName(specName)
    tasks.register<Jar>(jarTaskName) {
        archiveBaseName.set(specName)
        archiveClassifier.set("")
        destinationDirectory.set(layout.buildDirectory.dir("libs/pub"))
        val sourceDir = layout.buildDirectory.dir("classes/${specName}")
        from(sourceDir)
        dependsOn(tasks[compileTaskName])

        doFirst {
            logger.lifecycle("Building JAR $specName from compiled classes ${sourceDir.get().asFile}")
        }
    }
}

tasks.register("generateAllOpenApi") {
    dependsOn(generatedTasks)
}

tasks.compileJava {
    dependsOn(tasks.named("generateAllOpenApi"))
}

/*
   =================NEXUS PUBLISH=================
*/

file(".env").takeIf { it.exists() }?.readLines()?.forEach {
    var (k, v) = it.split("=", limit = 2)
    System.setProperty(k.trim(), v.trim())
    logger.lifecycle("${k.trim()}=${v.trim()}")
}

val nexusUrl = System.getenv("NEXUS_URL") ?: System.getProperty("NEXUS_URL")
val nexusUser = System.getenv("NEXUS_USERNAME") ?: System.getProperty("NEXUS_USERNAME")
val nexusPassword = System.getenv("NEXUS_PASSWORD") ?: System.getProperty("NEXUS_PASSWORD")

if (nexusUrl.isNullOrBlank() || nexusUser.isNullOrBlank() || nexusPassword.isNullOrBlank()) {
    throw GradleException(
        "NEXUS details are not set. Create a .env file with correct properties: " +
                "NEXUS_URL, NEXUS_USERNAME, NEXUS_PASSWORD"
    )
}

publishing {
    publications {
        specifications.forEach { spec ->
            val specName = spec.nameWithoutExtension
            val jarFile = file("build/libs/pub").listFiles()
                ?.firstOrNull { it.name.contains(specName) && (it.extension == "jar" || it.extension == "zip") }
            if (jarFile != null) {
                val publishTaskName = buildPublishTaskName(specName)
                logger.lifecycle("Found JAR ${jarFile.name} for publishing to nexus repository")
                create<MavenPublication>(publishTaskName) {
                    artifact(jarFile)
                    groupId = project.group.toString()
                    artifactId = specName
                    version = project.version.toString()

                    pom {
                        name.set("Generated API $specName")
                        description.set("Generated code for $specName")
                    }
                }
            }
        }
    }
    repositories {
        maven {
            name = "nexus"
            url = uri(nexusUrl)
            isAllowInsecureProtocol = true
            credentials {
                username = nexusUser
                password = nexusPassword
            }
        }
    }
}