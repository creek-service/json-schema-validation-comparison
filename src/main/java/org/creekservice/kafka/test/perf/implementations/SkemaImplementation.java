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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.creekservice.kafka.test.perf.testsuite.SchemaSpec.DRAFT_2020_12;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.erosb.jsonsKema.JsonParser;
import com.github.erosb.jsonsKema.JsonValue;
import com.github.erosb.jsonsKema.SchemaLoader;
import com.github.erosb.jsonsKema.SchemaLoaderConfig;
import com.github.erosb.jsonsKema.ValidationFailure;
import com.github.erosb.jsonsKema.Validator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;
import org.creekservice.kafka.test.perf.model.TestModel;
import org.creekservice.kafka.test.perf.testsuite.AdditionalSchemas;
import org.creekservice.kafka.test.perf.testsuite.SchemaSpec;

/**
 * SkemaSerde impl.
 *
 * <p>Unfortunately, the validator library requires the JSON to be parsed using its own parser. This
 * requires an additional parse step on serialization and deserialization: an additional cost.
 */
@SuppressWarnings("FieldMayBeFinal") // not final to avoid folding.
public class SkemaImplementation implements Implementation {

    private static final MetaData METADATA =
            new MetaData("erosb/json-sKema", "sKema", Set.of(DRAFT_2020_12));

    private ObjectMapper mapper = JsonMapper.builder().build();

    @Override
    public MetaData metadata() {
        return METADATA;
    }

    @Override
    public JsonValidator prepare(
            final String schema, final SchemaSpec spec, final AdditionalSchemas additionalSchemas) {
        final JsonValue schemaJson = new JsonParser(schema).parse();

        final SchemaLoader schemaLoader =
                new SchemaLoader(
                        schemaJson,
                        new SchemaLoaderConfig(
                                uri ->
                                        new ByteArrayInputStream(
                                                additionalSchemas.load(uri).getBytes(UTF_8))));

        final Validator validator = Validator.forSchema(schemaLoader.load());

        return new JsonValidator() {
            @Override
            public void validate(final String json) {
                final JsonValue instance = new JsonParser(json).parse();
                final ValidationFailure failure = validator.validate(instance);
                if (failure != null) {
                    throw new RuntimeException(failure.getMessage());
                }
            }

            @Override
            public byte[] serialize(final TestModel model, final boolean validate) {
                try {
                    final String json = mapper.writeValueAsString(model);
                    if (validate) {
                        validate(json);
                    }
                    return json.getBytes(UTF_8);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public TestModel deserialize(final byte[] data) {
                try {
                    final String json = new String(data, UTF_8);
                    validate(json);
                    return mapper.readValue(json, TestModel.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
