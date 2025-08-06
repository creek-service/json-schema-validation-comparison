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

package org.creekservice.kafka.test.perf.performance.util;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.creekservice.kafka.test.perf.performance.util.model.PerformanceResult;

final class PerformanceJsonReader {

    PerformanceResult[] read(final Path jsonResult) {
        return parseJson(readJson(jsonResult));
    }

    private static String readJson(final Path jsonResult) {
        try {
            return Files.readString(jsonResult, UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read from " + jsonResult, e);
        }
    }

    static PerformanceResult[] parseJson(final String jsonResult) {
        try {
            final ObjectMapper mapper =
                    JsonMapper.builder()
                            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                            .build();
            return mapper.readValue(jsonResult, PerformanceResult[].class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read from " + jsonResult, e);
        }
    }
}
