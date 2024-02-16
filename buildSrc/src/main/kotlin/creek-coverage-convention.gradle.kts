/*
 * Copyright 2022-2023 Creek Contributors (https://github.com/creek-service)
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

/**
 * Standard coverage configuration of Creek projects, utilising Jacoco and Coveralls.io
 *
 * <p>Versions:
 *  - 1.3: remove deprecated use of $buildDir
 *  - 1.2: Apply to root project only
 */

plugins {
    java
    jacoco
    id("com.github.kt3k.coveralls")
}

repositories {
    mavenCentral()
}

allprojects {
    apply(plugin = "java")

    tasks.withType<JacocoReport>().configureEach{
        dependsOn(tasks.test)
    }
}

val coverage = tasks.register<JacocoReport>("coverage") {
    group = "creek"
    description = "Generates an aggregate code coverage report"

    val coverageReportTask = this

    allprojects {
        val proj = this
        // Roll results of each test task into the main coverage task:
        proj.tasks.matching { it.extensions.findByType<JacocoTaskExtension>() != null }.forEach {
            coverageReportTask.sourceSets(proj.sourceSets.main.get())
            coverageReportTask.executionData(it.extensions.findByType<JacocoTaskExtension>()!!.destinationFile)
            coverageReportTask.dependsOn(it)
        }
    }

    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

coveralls {
    sourceDirs = allprojects.flatMap{it.sourceSets.main.get().allSource.srcDirs}.map{it.toString()}
    jacocoReportPath = layout.buildDirectory.file("reports/jacoco/coverage/coverage.xml")
}

tasks.coveralls {
    group = "creek"
    description = "Uploads the aggregated coverage report to Coveralls"

    dependsOn(coverage)
    onlyIf{System.getenv("CI") != null}
}