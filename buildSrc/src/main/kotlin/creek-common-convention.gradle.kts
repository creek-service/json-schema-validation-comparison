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
 * Standard configuration of Creek projects
 *
 * <p>Apply to all java modules, usually excluding the root project in multi-module sets.
 *
 * <p>Versions:
 *  - 1.10: Add ability to exclude containerised tests
 *  - 1.9: Add `allDeps` task.
 *  - 1.8: Tweak test config to reduce build speed.
 *  - 1.7: Switch to setting Java version via toolchain
 *  - 1.6: Remove GitHub packages for snapshots
 *  - 1.5: Add filters to exclude generated sources
 *  - 1.4: Add findsecbugs-plugin
 */

plugins {
    java
    checkstyle
    id("com.github.spotbugs")
    id("com.diffplug.spotless")
}

group = "org.creekservice"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

repositories {
    mavenCentral()

    maven {
        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        mavenContent {
            includeGroup("org.creekservice")
            snapshotsOnly()
        }
    }
}

dependencies {
    spotbugsPlugins("com.h3xstream.findsecbugs:findsecbugs-plugin:1.12.0")
}

configurations.all {
    // Reduce chance of build servers running into compilation issues due to stale snapshots:
    resolutionStrategy.cacheChangingModulesFor(15, TimeUnit.MINUTES)
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:all,-serial,-requires-automatic,-requires-transitive-automatic,-module")
    options.compilerArgs.add("-Werror")
}

tasks.test {
    useJUnitPlatform() {
        if (project.hasProperty("excludeContainerised")) {
            excludeTags("ContainerisedTest")
        }
    }

    setForkEvery(5)
    maxParallelForks = Runtime.getRuntime().availableProcessors()
    testLogging {
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showCauses = true
        showExceptions = true
        showStackTraces = true
    }
}

spotless {
    java {
        googleJavaFormat("1.15.0").aosp().reflowLongStrings()
        indentWithSpaces()
        importOrder()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
        toggleOffOn("formatting:off", "formatting:on")
        targetExclude("**/build/generated/source*/**/*.*")
    }
}

spotbugs {
    excludeFilter.set(rootProject.file("config/spotbugs/suppressions.xml"))

    tasks.spotbugsMain {
        reports.create("html") {
            required.set(true)
            setStylesheet("fancy-hist.xsl")
        }
    }
    tasks.spotbugsTest {
        reports.create("html") {
            required.set(true)
            setStylesheet("fancy-hist.xsl")
        }
    }
}

if (rootProject.name != project.name) {
    tasks.jar {
        archiveBaseName.set("${rootProject.name}-${project.name}")
    }
}

val format = tasks.register("format") {
    group = "creek"
    description = "Format the code"

    dependsOn("spotlessCheck", "spotlessApply")
}

val static = tasks.register("static") {
    group = "creek"
    description = "Run static code analysis"

    dependsOn("checkstyleMain", "checkstyleTest", "spotbugsMain", "spotbugsTest")

    shouldRunAfter(format)
}

tasks.test {
    shouldRunAfter(static)
}

// See: https://solidsoft.wordpress.com/2014/11/13/gradle-tricks-display-dependencies-for-all-subprojects-in-multi-project-build/
tasks.register<DependencyReportTask>("allDeps") {}

