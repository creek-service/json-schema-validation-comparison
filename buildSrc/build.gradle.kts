/*
 * Copyright 2023-2025 Creek Contributors (https://github.com/creek-service)
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
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

val jvmTargetVer = JavaLanguageVersion.of(17)

java {
    toolchain.languageVersion.set(jvmTargetVer)
}

kotlin {
    jvmToolchain {
        languageVersion.set(jvmTargetVer)
    }
}


dependencies {
    implementation("com.github.spotbugs.snom:spotbugs-gradle-plugin:6.4.8")                // https://plugins.gradle.org/plugin/com.github.spotbugs
    implementation("com.diffplug.spotless:spotless-plugin-gradle:8.4.0")                   // https://plugins.gradle.org/plugin/com.diffplug.spotless
    implementation("org.javamodularity:moduleplugin:2.0.0")                                // https://plugins.gradle.org/plugin/org.javamodularity.moduleplugin
    implementation("io.github.gradle-nexus:publish-plugin:2.0.0")                           // https://plugins.gradle.org/plugin/io.github.gradle-nexus.publish-plugin
    implementation("com.gradle.publish:plugin-publish-plugin:2.1.1")                        // https://plugins.gradle.org/plugin/com.gradle.plugin-publish
}