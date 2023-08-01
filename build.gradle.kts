/*
 * Copyright 2023 Creek Contributors (https://github.com/creek-service)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    java
    jacoco
    `creek-common-convention`
    `creek-module-convention`
    `creek-coverage-convention`
    `creek-publishing-convention`
    `creek-sonatype-publishing-convention`
    id("pl.allegro.tech.build.axion-release") version "1.15.4" // https://plugins.gradle.org/plugin/pl.allegro.tech.build.axion-release
}

project.version = scmVersion.version

allprojects {
    tasks.jar {
        onlyIf { sourceSets.main.get().allSource.files.isNotEmpty() }
    }
}

val creekVersion = "0.4.2-SNAPSHOT"
val guavaVersion = "32.1.2-jre"         // https://mvnrepository.com/artifact/com.google.guava/guava
val log4jVersion = "2.20.0"           // https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-core
val junitVersion = "5.10.0"            // https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api
val junitPioneerVersion = "2.0.1"     // https://mvnrepository.com/artifact/org.junit-pioneer/junit-pioneer
val mockitoVersion = "5.4.0"          // https://mvnrepository.com/artifact/org.mockito/mockito-junit-jupiter
val hamcrestVersion = "2.2"           // https://mvnrepository.com/artifact/org.hamcrest/hamcrest-core

dependencies {
    testImplementation("org.creekservice:creek-test-hamcrest:$creekVersion")
    testImplementation("org.creekservice:creek-test-util:$creekVersion")
    testImplementation("org.creekservice:creek-test-conformity:$creekVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testImplementation("org.junit-pioneer:junit-pioneer:$junitPioneerVersion")
    testImplementation("org.mockito:mockito-junit-jupiter:$mockitoVersion")
    testImplementation("org.hamcrest:hamcrest-core:$hamcrestVersion")
    testImplementation("com.google.guava:guava-testlib:$guavaVersion")
    testImplementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    testImplementation("org.apache.logging.log4j:log4j-slf4j2-impl:$log4jVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

defaultTasks("format", "static", "check")
