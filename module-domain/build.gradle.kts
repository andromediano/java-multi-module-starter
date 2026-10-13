/*
 * Copyright 2019. OPENTEST. All rights reserved.
 */

import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
  id("library-conventions")
  alias(libs.plugins.spring.boot)
  alias(libs.plugins.spring.dependency.management)
  alias(libs.plugins.freefair.lombok)
}

tasks.named<BootJar>("bootJar") {
  enabled = false
}

tasks.named<Jar>("jar") {
  enabled = true
}

tasks.named<JavaCompile>("compileJava") {
  options.compilerArgs.add("-processor")
  options.compilerArgs.add(
    listOf(
      "lombok.launch.AnnotationProcessorHider\$AnnotationProcessor",
      "lombok.launch.AnnotationProcessorHider\$ClaimingProcessor",
      "com.querydsl.apt.jpa.JPAAnnotationProcessor",
      "org.mapstruct.ap.MappingProcessor",
    ).joinToString(","),
  )
}

dependencies {

  // 1. 내부 프로젝트 의존성
  api(project(":module-core"))

  // 2. 외부 라이브러리 및 프레임워크
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.boot:spring-boot-starter-webmvc")
  implementation(libs.guava)
  implementation(libs.querydsl.apt)
  implementation(libs.querydsl.jpa)
  implementation(libs.mapstruct)

  //
  // 3. annotationProcessor
  //
  annotationProcessor(libs.mapstruct.processor)
  annotationProcessor(libs.querydsl.apt)
  annotationProcessor("jakarta.persistence:jakarta.persistence-api")
  annotationProcessor("jakarta.annotation:jakarta.annotation-api")

  // 4. 런타임 및 DB 드라이버
  runtimeOnly(libs.postgresql)

  // 5. 테스트 관련 (Test Scope)
  testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
  testAnnotationProcessor(libs.mapstruct.processor)
}
