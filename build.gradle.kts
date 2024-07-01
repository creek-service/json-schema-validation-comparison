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
    id("org.ajoberstar.grgit.service") version "5.2.2"
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
val junitVersion = "5.10.3"
val junitPioneerVersion = "2.2.0"
val mockitoVersion = "5.12.0"
val jmhVersion = "1.37"
val confluentVersion = "7.6.1"
val vertxVersion = "4.5.8"

dependencies {
    implementation("org.openjdk.jmh:jmh-core:$jmhVersion")
    annotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:$jmhVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.github.spotbugs:spotbugs-annotations:4.8.5")
    implementation("org.creekservice:creek-test-util:$creekVersion")
    implementation("org.ow2.asm:asm:9.7")

    implementation("org.json:json:20240303")

    implementation("com.worldturner.medeia:medeia-validator-jackson:1.1.1")

    implementation("com.github.erosb:everit-json-schema:1.14.4")

    implementation("com.github.erosb:json-sKema:0.15.0")

    implementation("io.confluent:kafka-streams-json-schema-serde:$confluentVersion")
    implementation("io.confluent:kafka-schema-registry-client:$confluentVersion")

    implementation("io.vertx:vertx-json-schema:$vertxVersion")
    compileOnly("io.vertx:vertx-codegen:$vertxVersion")

    implementation("net.jimblackler.jsonschemafriend:core:0.12.4")

    implementation("com.networknt:json-schema-validator:1.4.2")

    implementation("com.qindesign:snowy-json:0.16.0")
    runtimeOnly("org.glassfish:jakarta.json:2.0.0:module")

    implementation("org.leadpony.justify:justify:3.1.0")

    implementation("dev.harrel:json-schema:1.6.0")
    implementation("com.sanctionco.jmail:jmail:1.6.3") // dev.harrel format validation

    runtimeOnly("org.slf4j:slf4j-nop:2.0.13")

    testImplementation("org.creekservice:creek-test-hamcrest:$creekVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testImplementation("org.junit-pioneer:junit-pioneer:$junitPioneerVersion")
    testImplementation("org.mockito:mockito-junit-jupiter:$mockitoVersion")
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
    dependsOn(pullTask)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("org.creekservice.kafka.test.perf.FunctionalMain")
    args = listOf(jsonSchemaTestSuiteDir.get().asFile.absolutePath)
}

tasks.register<JavaExec>("runValidateBenchmark") {
    dependsOn(pullTask)
    classpath = sourceSets.main.get().runtimeClasspath
    configureBenchmarkTask("JsonValidateBenchmark", false)
}

tasks.register<JavaExec>("runSerdeBenchmark") {
    classpath = sourceSets.main.get().runtimeClasspath
    configureBenchmarkTask("JsonSerdeBenchmark", false)
}

tasks.register("runBenchmarks") {
    dependsOn("runValidateBenchmark", "runSerdeBenchmark")
}

val runValidateBenchmarkSmokeTest = tasks.register<JavaExec>("runValidateBenchmarkSmokeTest") {
    dependsOn(pullTask)
    classpath = sourceSets.main.get().runtimeClasspath
    configureBenchmarkTask("JsonValidateBenchmark", true)
}

val runSerdeBenchmarkSmokeTest = tasks.register<JavaExec>("runSerdeBenchmarkSmokeTest") {
    classpath = sourceSets.main.get().runtimeClasspath
    configureBenchmarkTask("JsonSerdeBenchmark", true)
}

val runBenchmarkSmokeTest = tasks.register("runBenchmarkSmokeTest") {
    dependsOn(runValidateBenchmarkSmokeTest, runSerdeBenchmarkSmokeTest)
}

fun JavaExec.configureBenchmarkTask(benchmarkClass: String, smokeTest: Boolean) {
    mainClass.set("org.creekservice.kafka.test.perf.PerformanceMain")

    outputs.file(file("docs/_includes/$benchmarkClass.json"))
    outputs.file(file("docs/_includes/$benchmarkClass.md"))

    args(
        listOf(
            // Benchmark to run:
            benchmarkClass
        )
    )

    if (smokeTest) {
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
    }
}

val extractImplementations = tasks.register<JavaExec>("extractImplementations") {
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("org.creekservice.kafka.test.perf.ImplementationsMain")
}

tasks.check {
    dependsOn(runFunctionalTests, runBenchmarkSmokeTest, extractImplementations)
}

tasks.register("buildTestIncludes") {
    description = "Build include files needed to generate the Jekyll website"
    dependsOn(runFunctionalTests, runBenchmarkSmokeTest, extractImplementations)
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
