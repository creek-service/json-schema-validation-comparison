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
 * Standard configuration for Creek library publishing
 *
 * <p>Version: 1.2
 *
 * <p> Apply this plugin only to subprojects if in multi-module setup.
 *
 * <p> Use `creek-plugin-publishing-convention` for Gradle plugins.
 */

plugins {
    java
    signing
    `maven-publish`
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).apply {
            addBooleanOption("html5", true)
            // Why -quite? See: https://github.com/gradle/gradle/issues/2354
            addStringOption("Xwerror", "-quiet")
        }
    }
}

// Dummy/empty publishPlugins task, to allow consistent build.yml workflow
tasks.register("publishPlugins") {
    doLast {
        logger.info("No Gradle plugins to publish")
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/creek-service/${rootProject.name}")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            val prependRootName = rootProject.name != project.name
            if (prependRootName) {
                artifactId = "${rootProject.name}-${project.name}"
            }

            pom {
                name.set("${project.group}:${artifactId}")

                if (prependRootName) {
                    description.set("${rootProject.name.capitalize()} ${project.name} library".replace("-", " "))
                } else {
                    description.set("${project.name.capitalize()} library".replace("-", " "))
                }

                url.set("https://www.creekservice.org")
                inceptionYear.set("2022")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                organization {
                    name.set("Creek Service")
                    url.set("https://www.creekservice.org")
                }

                developers {
                    developer {
                        name.set("Andy Coates")
                        email.set("8012398+big-andy-coates@users.noreply.github.com")
                        organization.set("Creek Service")
                        organizationUrl.set("https://www.creekservice.org")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/creek-service/${rootProject.name}.git")
                    developerConnection.set("scm:git:ssh://github.com/creek-service/${rootProject.name}.git")
                    url.set("https://github.com/creek-service/${rootProject.name}")
                }

                issueManagement {
                    name.set("GitHub issues")
                    url.set("https://github.com/creek-service/${rootProject.name}/issues")
                }
            }
        }
    }
}

/**
 * Configure signing of publication artifacts.
 *
 * <p>Can be skipped for snapshot builds.<p>
 *
 * <p>Even snapshot builds will be signed if signing credentials are available.
 * See https://central.sonatype.org/publish/publish-gradle/#credentials
 */
signing {
    setRequired {
        !project.version.toString().endsWith("-SNAPSHOT")
                && !project.hasProperty("skipSigning")
    }

    if (project.hasProperty("signingKey")) {
        useInMemoryPgpKeys(properties["signingKey"].toString(), properties["signingPassword"].toString())
    }

    sign(publishing.publications["mavenJava"])
}
