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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.List;
import java.util.stream.Collectors;
import org.creekservice.kafka.test.perf.implementations.Implementation;

public final class ImplsJsonFormatter {

    public static String implDetailsAsJson(final List<Implementation> impls) {
        final ObjectMapper mapper = JsonMapper.builder().build();

        final List<Implementation.MetaData> metadata =
                impls.stream().map(Implementation::metadata).collect(Collectors.toList());

        try {
            return mapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private ImplsJsonFormatter() {}
}
