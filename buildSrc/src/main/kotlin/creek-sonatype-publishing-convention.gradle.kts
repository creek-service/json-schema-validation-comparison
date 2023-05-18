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
 * Standard configuration for Creek library publishing to Maven Central vis SonaType OSSRH
 *
 * <p>Version: 1.1
 *
 * <p>Apply this plugin only to the root project if in multi-module setup.
 *
 * @see <a href="https://s01.oss.sonatype.org/">OSSHR Nexus Service</a>
 */

plugins {
    id("io.github.gradle-nexus.publish-plugin")
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            stagingProfileId.set("89a20518f39cd")

            if (project.hasProperty("SONA_USERNAME")) {
                username.set(project.property("SONA_USERNAME").toString())
            }

            if (project.hasProperty("SONA_PASSWORD")) {
                password.set(project.property("SONA_PASSWORD").toString())
            }
        }
    }
}
