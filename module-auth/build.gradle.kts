/*
 * Copyright 2019. OPENTEST. All rights reserved.
 */

plugins {
    id("common-conventions")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.freefair.lombok)
}

dependencies {

  // 1. 내부 프로젝트 의존성
  implementation(project(":module-core"))
  implementation(project(":module-domain"))

  // 2. OAuth2 Authorization Server
  implementation("org.springframework.boot:spring-boot-starter-oauth2-authorization-server")

  // 3. Web + Data JPA
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")

  // 4. 런타임 및 DB 드라이버
  runtimeOnly(libs.postgresql)

  // 5. 테스트
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.boot:spring-boot-webmvc-test")
  testImplementation("org.springframework.security:spring-security-test")
}
