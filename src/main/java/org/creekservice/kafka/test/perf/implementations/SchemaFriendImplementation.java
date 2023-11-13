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

import static org.creekservice.kafka.test.perf.testsuite.SchemaSpec.DRAFT_03;
import static org.creekservice.kafka.test.perf.testsuite.SchemaSpec.DRAFT_04;
import static org.creekservice.kafka.test.perf.testsuite.SchemaSpec.DRAFT_06;
import static org.creekservice.kafka.test.perf.testsuite.SchemaSpec.DRAFT_07;
import static org.creekservice.kafka.test.perf.testsuite.SchemaSpec.DRAFT_2019_09;
import static org.creekservice.kafka.test.perf.testsuite.SchemaSpec.DRAFT_2020_12;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.awt.Color;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.jimblackler.jsonschemafriend.Loader;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaStore;
import net.jimblackler.jsonschemafriend.ValidationException;
import net.jimblackler.jsonschemafriend.Validator;
import org.creekservice.kafka.test.perf.model.TestModel;
import org.creekservice.kafka.test.perf.testsuite.AdditionalSchemas;
import org.creekservice.kafka.test.perf.testsuite.SchemaSpec;

@SuppressWarnings("FieldMayBeFinal") // not final to avoid folding.
public class SchemaFriendImplementation implements Implementation {

    private static final MetaData METADATA =
            new MetaData(
                    "JSON Schema Friend",
                    "SchemaFriend",
                    Language.Java,
                    Licence.Apache_v2_0,
                    Set.of(DRAFT_2020_12, DRAFT_2019_09, DRAFT_07, DRAFT_06, DRAFT_04, DRAFT_03),
                    "https://github.com/jimblackler/jsonschemafriend",
                    new Color(255, 159, 64));

    private ObjectMapper mapper = JsonMapper.builder().build();

    @Override
    public MetaData metadata() {
        return METADATA;
    }

    @Override
    public JsonValidator prepare(
            final String schema, final SchemaSpec spec, final AdditionalSchemas additionalSchemas) {
        final Schema parsedSchema = parseSchema(schema, spec, additionalSchemas::load);
        final Validator validator = new Validator(true);

        return new JsonValidator() {
            @Override
            public void validate(final String json) {
                try {
                    final Object o = mapper.readValue(json, Object.class);
                    validator.validate(parsedSchema, o, URI.create(""));
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public byte[] serialize(final TestModel model, final boolean validate) {
                try {
                    final Map<String, Object> map =
                            mapper.convertValue(model, new TypeReference<>() {});

                    if (validate) {
                        validator.validate(parsedSchema, map);
                    }

                    return mapper.writeValueAsBytes(map);
                } catch (JsonProcessingException | ValidationException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public TestModel deserialize(final byte[] data) {
                try {
                    final Map<String, Object> map =
                            mapper.readValue(data, new TypeReference<>() {});

                    validator.validate(parsedSchema, map);

                    return mapper.convertValue(map, TestModel.class);
                } catch (ValidationException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @SuppressWarnings("unchecked")
    private Schema parseSchema(
            final String schema,
            final SchemaSpec spec,
            final Function<URI, String> additionalSchemas) {
        try {
            final Loader loader =
                    (uri, cacheSchema) -> {
                        try {
                            return additionalSchemas.apply(uri);
                        } catch (final UnsupportedOperationException e) {
                            throw new IOException(e);
                        }
                    };

            final SchemaStore schemaStore = new SchemaStore(url -> url, true, loader);

            final Object parsed = mapper.readValue(schema, Object.class);
            if (parsed instanceof Map) {
                final Map<String, Object> schemaMap = (Map<String, Object>) parsed;
                if (!schemaMap.containsKey("$schema")) {
                    schemaMap.put("$schema", spec.uri().toString());
                }
            }

            return schemaStore.loadSchema(parsed, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Final, empty finalize method stops spotbugs CT_CONSTRUCTOR_THROW
    // Can be moved to base type after https://github.com/spotbugs/spotbugs/issues/2665
    @Override
    @SuppressWarnings({"deprecation", "Finalize"})
    protected final void finalize() {}
}
