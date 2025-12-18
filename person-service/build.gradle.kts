import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

var properties = mapOf(
    "springDocVersion" to "2.5.0",
    "logbackEncoderVersion" to "8.0",
    "testContainerVersion" to "1.21.3",
    "openFeignVersion" to "4.3.0",
    "flywayVersion" to "11.14.0",
    "postgresVersion" to "42.7.7",
    "testContainersKeycloakVersion" to "3.4.0",
    "keycloakAdminVersion" to "12.0.2"
)

plugins {
    java
    idea
    `maven-publish`
    id("org.springframework.boot") version ("3.5.0")
    id("org.openapi.generator") version ("7.14.0")
    id("io.spring.dependency-management") version ("1.1.6")
}

group = "com.example"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    // SPRING
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign:${properties["openFeignVersion"]}")
    implementation("org.springframework.data:spring-data-envers")

    // OBSERVABILITY
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${properties["springDocVersion"]}")
    implementation("org.projectlombok:lombok")
    implementation("net.logstash.logback:logstash-logback-encoder:${properties["logbackEncoderVersion"]}")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.micrometer:micrometer-tracing")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    annotationProcessor("org.projectlombok:lombok")

    // UTIL
    implementation("org.aspectj:aspectjweaver")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${properties["springDocVersion"]}")
    implementation("org.projectlombok:lombok")
    implementation("net.logstash.logback:logstash-logback-encoder:${properties["logbackEncoderVersion"]}")
    annotationProcessor("org.projectlombok:lombok")

    // DB
    implementation("org.flywaydb:flyway-core:${properties["flywayVersion"]}")
    implementation("org.postgresql:postgresql:${properties["postgresVersion"]}")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:${properties["flywayVersion"]}")

    // TEST
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("com.github.dasniko:testcontainers-keycloak:${properties["testContainersKeycloakVersion"]}")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.keycloak:keycloak-admin-client:${properties["keycloakAdminVersion"]}")
    testImplementation("org.testcontainers:junit-jupiter:${properties["testContainerVersion"]}")
    testImplementation("org.testcontainers:postgresql:${properties["testContainerVersion"]}")
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

//tasks.named("build") {
//    dependsOn(jars)
//}

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
        destinationDirectory.set(layout.buildDirectory.dir("libs"))
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

publishing {
    publications {
        specifications.forEach { spec ->
            val specName = spec.nameWithoutExtension
            val jarFile = file("build/libs").listFiles()
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
            val snapshotUri = uri("http://localhost:8800/repository/maven-snapshots/")
            val releasesUri = uri("http://localhost:8800/repository/maven-releases/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotUri else releasesUri
            isAllowInsecureProtocol = true
            credentials {
                username = "admin"
                password = "admin"
            }
        }
    }
}
