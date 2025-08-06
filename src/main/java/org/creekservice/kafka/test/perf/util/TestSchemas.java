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

package org.creekservice.kafka.test.perf.util;

import java.nio.file.Path;
import org.creekservice.api.test.util.TestPaths;

public final class TestSchemas {
    private static final Path RESOURCE_ROOT =
            TestPaths.moduleRoot("json-schema-validation-comparison").resolve("src/main/resources");
    public static final String DRAFT_2020_SCHEMA =
            TestPaths.readString(RESOURCE_ROOT.resolve("schema-draft-2020-12.json"));
    public static final String DRAFT_7_SCHEMA =
            TestPaths.readString(RESOURCE_ROOT.resolve("schema-draft-7.json"));

    private TestSchemas() {}
}
