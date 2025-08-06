/*
 * Copyright 2022-2025 Creek Contributors (https://github.com/creek-service)
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
 * Standard configuration of Java Module Platform System, a.k.a. Java 9 modules.
 *
 * <p>Version: 1.1
 *
 * <p>Apply to all modules that publish JPMS modules.
 */

plugins {
    java
    id("org.javamodularity.moduleplugin")
}

java {
    modularity.inferModulePath.set(false)
}
