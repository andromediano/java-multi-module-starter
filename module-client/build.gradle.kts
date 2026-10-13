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

  // 1. Web + OAuth2 Client
  implementation("org.springframework.boot:spring-boot-starter-webmvc")
  implementation("org.springframework.boot:spring-boot-starter-security-oauth2-client")

  // 2. Thymeleaf
  implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

  // 3. 테스트
  testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
  testImplementation("org.springframework.boot:spring-boot-starter-security-oauth2-client-test")
}
