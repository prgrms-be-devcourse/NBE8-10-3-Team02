plugins {
	java
	jacoco
	id("org.springframework.boot") version "3.5.10"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("jvm") version "2.1.0"
	kotlin("plugin.spring") version "2.1.0"
	kotlin("plugin.jpa") version "2.1.0"
}

group = "com"
version = "0.0.1-SNAPSHOT"
description = "Demo project for Spring Boot"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("io.jsonwebtoken:jjwt-api:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

	implementation("org.springframework.boot:spring-boot-starter-batch")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")

	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")

	developmentOnly("org.springframework.boot:spring-boot-devtools")

	runtimeOnly("org.postgresql:postgresql")
//	runtimeOnly("com.h2database:h2")
//	developmentOnly("org.springframework.boot:spring-boot-h2console")

	// Flyway
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")

	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	implementation("org.springframework.boot:spring-boot-starter-security")
	testImplementation("org.springframework.security:spring-security-test")

	// 1. QueryDSL 라이브러리
	implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")

	// 2. QClass 생성을 위한 핵심 엔진 (이 3개가 세트입니다)
	annotationProcessor("com.querydsl:querydsl-apt:5.1.0:jakarta")
	annotationProcessor("jakarta.persistence:jakarta.persistence-api")
	annotationProcessor("jakarta.annotation:jakarta.annotation-api")

	//caffeine
	implementation("org.springframework.boot:spring-boot-starter-cache")
	implementation("com.github.ben-manes.caffeine:caffeine")
	testImplementation("org.springframework.boot:spring-boot-starter-test")

	//Testcontainers
	testImplementation(platform("org.testcontainers:testcontainers-bom:1.20.4"))
	testImplementation("org.testcontainers:postgresql")
	testImplementation("org.testcontainers:junit-jupiter")

	//RestClient test용
	testImplementation(platform("com.squareup.okhttp3:okhttp-bom:5.3.2"))
	testImplementation("com.squareup.okhttp3:mockwebserver")

	//Resilience4j (circuit breaker, retry, rate limiter)
	implementation("io.github.resilience4j:resilience4j-spring-boot3:2.3.0")
	implementation("io.github.resilience4j:resilience4j-micrometer:2.3.0")
	implementation("org.aspectj:aspectjweaver")

	// Actuator + Prometheus (메트릭 수집)
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("io.micrometer:micrometer-registry-prometheus")

	// pgvector
	implementation("com.pgvector:pgvector:0.1.6")

}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.jacocoTestReport {
	dependsOn(tasks.test)
	reports {
		xml.required = true
		html.required = true
	}
}


sourceSets {
	main {
		java {
			srcDirs("build/generated/sources/annotationProcessor/java/main")
		}
	}
}
