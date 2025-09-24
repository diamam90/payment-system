plugins {
    id("java")
    id("org.springframework.boot") version ("3.5.0")
    id("org.openapi.generator") version ("7.14.0")
    id("io.spring.dependency-management") version ("1.1.6")
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("org.aspectj:aspectjweaver")

    implementation("io.micrometer:micrometer-registry-prometheus")

    implementation("org.projectlombok:lombok")
    implementation("org.openapitools:jackson-databind-nullable:0.2.6")
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")

    annotationProcessor("org.projectlombok:lombok")

    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.github.dasniko:testcontainers-keycloak:3.4.0")
    testImplementation("org.testcontainers:junit-jupiter:1.21.3")
}


tasks.compileJava {
    dependsOn(tasks.openApiGenerate)
}

tasks.test {
    useJUnitPlatform()
}

openApiGenerate {
    generatorName.set("java")
    inputSpec.set("$projectDir/openapi/individuals-api.yaml")
    outputDir.set("${project.layout.buildDirectory.asFile.get()}/generated-sources/openapi")
    typeMappings.set(mapOf("DateTime" to "ZonedDateTime"))
    importMappings.set(mapOf("ZonedDateTime" to "java.time.ZonedDateTime"))
    configOptions.put("useJakartaEe", "true")
    configOptions.put("useBeanValidation", "true")
    configOptions.put("library", "webclient")
    configOptions.put("dateLibrary","java8")
    apiPackage.set("com.example.api")
    modelPackage.set("com.example.dto")
}

sourceSets {
    main {
        java {
            srcDir("${layout.buildDirectory.get().asFile}/generated-sources/openapi/src/main/java")
        }
    }
}

