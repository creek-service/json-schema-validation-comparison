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

package org.creekservice.kafka.test.perf.implementations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.awt.Color;
import java.io.IOException;
import java.util.Set;
import org.creekservice.kafka.test.perf.model.TestModel;
import org.creekservice.kafka.test.perf.testsuite.AdditionalSchemas;
import org.creekservice.kafka.test.perf.testsuite.SchemaSpec;

@SuppressWarnings("FieldMayBeFinal") // not final to avoid folding.
public class JacksonImplementation implements Implementation {

    private static final MetaData METADATA =
            new MetaData(
                    "Plain Jackson Serialization",
                    "Jackson",
                    Language.Java,
                    Licence.Apache_v2_0,
                    Set.of(SchemaSpec.DRAFT_07, SchemaSpec.DRAFT_2020_12),
                    "https://github.com/FasterXML/jackson-core",
                    new Color(20, 84, 166));

    private ObjectMapper mapper = JsonMapper.builder().build();

    @Override
    public MetaData metadata() {
        return METADATA;
    }

    @Override
    public JsonValidator prepare(
            final String schema, final SchemaSpec spec, final AdditionalSchemas additionalSchemas) {
        return new JsonValidator() {
            @Override
            public void validate(final String json) {
                throw new UnsupportedOperationException();
            }

            @Override
            public byte[] serialize(final TestModel model, final boolean validate) {
                try {
                    return mapper.writeValueAsBytes(model);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public TestModel deserialize(final byte[] data) {
                try {
                    return mapper.readValue(data, TestModel.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
