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
    `creek-common-convention`
    id("org.ajoberstar.grgit.service") version "5.2.0"
}

repositories {
    maven {
        url = uri("https://packages.confluent.io/maven/")
        group = "io.confluent"
    }

    maven {
        url = uri("https://jitpack.io")
    }
}

val creekVersion = "0.4.2-SNAPSHOT"
val log4jVersion = "2.20.0"
val junitVersion = "5.10.0"
val junitPioneerVersion = "2.0.1"
val mockitoVersion = "5.5.0"
val hamcrestVersion = "2.2"
val jmhVersion = "1.36"
val confluentVersion = "7.5.0"
val vertxVersion = "4.4.1"

dependencies {
    implementation("org.openjdk.jmh:jmh-core:$jmhVersion")
    annotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:$jmhVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind")

    implementation("org.json:json:20230227")

    implementation("com.worldturner.medeia:medeia-validator-jackson:1.1.0")

    implementation("com.github.erosb:everit-json-schema:1.14.2")

    implementation("com.github.erosb:json-sKema:0.6.0")

    implementation("io.confluent:kafka-streams-json-schema-serde:$confluentVersion")
    implementation("io.confluent:kafka-schema-registry-client:$confluentVersion")

    implementation("io.vertx:vertx-json-schema:$vertxVersion")
    compileOnly("io.vertx:vertx-codegen:$vertxVersion")

    implementation("net.jimblackler.jsonschemafriend:core:0.11.4")

    implementation("com.networknt:json-schema-validator:1.0.80"){
        exclude(group = "org.apache.commons", module = "commons-lang3")
    }

    implementation("com.qindesign:snowy-json:0.16.0")
    runtimeOnly("org.glassfish:jakarta.json:2.0.0:module")

    implementation("org.leadpony.justify:justify:3.1.0")

    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j2-impl:$log4jVersion")

    testImplementation("org.creekservice:creek-test-hamcrest:$creekVersion")
    implementation("org.creekservice:creek-test-util:$creekVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testImplementation("org.junit-pioneer:junit-pioneer:$junitPioneerVersion")
    testImplementation("org.mockito:mockito-junit-jupiter:$mockitoVersion")
    testImplementation("org.hamcrest:hamcrest-core:$hamcrestVersion")
    testImplementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    testImplementation("org.apache.logging.log4j:log4j-slf4j2-impl:$log4jVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:all,-serial,-requires-automatic,-requires-transitive-automatic,-module,-processing")
}

val jsonSchemaTestSuiteDir = layout.buildDirectory.dir("json-schema-test-suite")

val cloneTask = tasks.register("clone-json-schema-test-suite") {
    outputs.dir(jsonSchemaTestSuiteDir)

    onlyIf { !jsonSchemaTestSuiteDir.get().asFile.exists() }

    doLast {
        org.ajoberstar.grgit.Grgit.clone {
            dir = jsonSchemaTestSuiteDir.get().asFile
            uri = "https://github.com/json-schema-org/JSON-Schema-Test-Suite.git"
        }
    }
}

val pullTask = tasks.register("pull-json-schema-test-suite") {
    dependsOn(cloneTask)

    doLast {
        org.ajoberstar.grgit.Grgit.open {
            dir = jsonSchemaTestSuiteDir.get().asFile
        }.pull()
    }
}

val runFunctionalTests = tasks.register<JavaExec>("runFunctionalTests") {
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("org.creekservice.kafka.test.perf.testsuite.JsonTestSuiteMain")
    args = listOf(jsonSchemaTestSuiteDir.get().asFile.absolutePath)
    dependsOn(pullTask)
}

tasks.register<JavaExec>("runBenchmarks") {
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("org.creekservice.kafka.test.perf.BenchmarkRunner")
    args(listOf(
        // Output results in csv format
        "-rf", "csv",
        // To a named file
        "-rff", "benchmark_results.csv"
        // Todo: remove below:
        ,"-wi", "0",
        "-i", "1",
        "-t", "1",
        "-r", "1s",
        "-f", "0"
    ))
    dependsOn(pullTask)
}

val benchmarkSmokeTest = tasks.register<JavaExec>("runBenchmarkSmokeTest") {
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("org.creekservice.kafka.test.perf.BenchmarkRunner")
    args(listOf(
        // No warmup:
        "-wi", "0",
        // Single test iteration:
        "-i", "1",
        // On a single thread:
        "-t", "1",
        // Running for 1 second
        "-r", "1s",
        // With forking disabled, i.e. in-process
        "-f", "0"
    ))
    dependsOn(pullTask)
}

tasks.test {
    dependsOn(runFunctionalTests, benchmarkSmokeTest)
}

// Dummy / empty tasks required to allow the repo to use the same standard GitHub workflows as other Creek repos:
tasks.register("coveralls")
tasks.register("cV")
tasks.register("publish")
tasks.register("closeAndReleaseStagingRepository")
tasks.register("publishPlugins")

// Below is required until the following is fixed in IntelliJ:
// https://youtrack.jetbrains.com/issue/IDEA-316081/Gradle-8-toolchain-error-Toolchain-from-executable-property-does-not-match-toolchain-from-javaLauncher-property-when-different
gradle.taskGraph.whenReady {
    allTasks.filterIsInstance<JavaExec>().forEach {
        it.setExecutable(it.javaLauncher.get().executablePath.asFile.absolutePath)
    }
}

defaultTasks("format", "static", "check")
