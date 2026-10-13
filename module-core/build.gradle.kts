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

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webmvc")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("tools.jackson.core:jackson-databind")
}
