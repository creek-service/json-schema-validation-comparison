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
import static org.creekservice.kafka.test.perf.testsuite.SchemaSpec.DRAFT_04;
import static org.creekservice.kafka.test.perf.testsuite.SchemaSpec.DRAFT_06;
import static org.creekservice.kafka.test.perf.testsuite.SchemaSpec.DRAFT_07;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.creekservice.kafka.test.perf.model.TestModel;
import org.creekservice.kafka.test.perf.testsuite.AdditionalSchemas;
import org.creekservice.kafka.test.perf.testsuite.SchemaSpec;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * EveritSerde impl.
 *
 * <p>Unfortunately, the validator library requires the JSON to be parsed by org.JSON. As org.JSON
 * isn't really designed to convert between Java POJOs and JSON, e.g. it doesn't natively support
 * polymorphic types, using the library requires using Jackson and converting to org.JSON: an
 * additional cost.
 */
@SuppressWarnings("FieldMayBeFinal") // not final to avoid folding.
public class EveritImplementation implements Implementation {

    private static final MetaData METADATA =
            new MetaData(
                    "everit-org/json-schema",
                    "Everit",
                    Language.Java,
                    Licence.Apache_v2_0,
                    Set.of(DRAFT_04, DRAFT_06, DRAFT_07),
                    "https://github.com/everit-org/json-schema",
                    new Color(54, 162, 235),
                    org.everit.json.schema.loader.SchemaLoader.class,
                    "Deprecated. Replaced by Skema implementation");

    private ObjectMapper mapper = JsonMapper.builder().build();

    @Override
    public MetaData metadata() {
        return METADATA;
    }

    @Override
    public JsonValidator prepare(
            final String schema, final SchemaSpec spec, final AdditionalSchemas additionalSchemas) {

        final Object schemaObject = parse(schema);

        final Schema parsedSchema =
                schemaLoader(spec)
                        .schemaClient(
                                url ->
                                        new ByteArrayInputStream(
                                                additionalSchemas.load(url).getBytes(UTF_8)))
                        .schemaJson(schemaObject)
                        .build()
                        .load()
                        .build();

        return new JsonValidator() {
            @Override
            public void validate(final String json) {
                parseAndValidate(json);
            }

            @Override
            public byte[] serialize(final TestModel model, final boolean validate) {
                try {
                    final Map<String, Object> jsonNode =
                            mapper.convertValue(model, new TypeReference<>() {});
                    if (validate) {
                        final JSONObject jsonObject = new JSONObject(jsonNode);
                        parsedSchema.validate(jsonObject);
                    }
                    return mapper.writeValueAsBytes(jsonNode);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public TestModel deserialize(final byte[] data) {
                final JSONObject jsonObject =
                        (JSONObject) parseAndValidate(new String(data, UTF_8));
                return mapper.convertValue(jsonObject.toMap(), TestModel.class);
            }

            private Object parseAndValidate(final String json) {
                final Object jsonObject = parse(json);
                parsedSchema.validate(jsonObject);
                return jsonObject;
            }
        };
    }

    private Object parse(final String json) {
        try {
            final Object o = mapper.readValue(json, new TypeReference<>() {});
            if (o instanceof Map) {
                return new JSONObject((Map<?, ?>) o);
            }
            if (o instanceof Collection) {
                return new JSONArray((Collection<?>) o);
            }
            return o;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static SchemaLoader.SchemaLoaderBuilder schemaLoader(final SchemaSpec spec) {
        final SchemaLoader.SchemaLoaderBuilder builder = SchemaLoader.builder();

        switch (spec) {
            case DRAFT_07:
                return builder.draftV7Support();
            case DRAFT_06:
                return builder.draftV6Support();
            case DRAFT_04:
                return builder; // DRAFT 4 is the default.
            default:
                throw new RuntimeException("Unsupported draft: " + spec);
        }
    }
}
