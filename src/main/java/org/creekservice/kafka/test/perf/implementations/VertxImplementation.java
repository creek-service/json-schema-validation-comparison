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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.Draft;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.JsonSchemaOptions;
import io.vertx.json.schema.OutputFormat;
import io.vertx.json.schema.OutputUnit;
import io.vertx.json.schema.Validator;
import java.io.IOException;
import java.util.Map;
import org.creekservice.kafka.test.perf.model.TestModel;
import org.creekservice.kafka.test.perf.testsuite.AdditionalSchemas;
import org.creekservice.kafka.test.perf.testsuite.SchemaSpec;

@SuppressWarnings("FieldMayBeFinal") // not final to avoid folding.
public class VertxImplementation implements Implementation {

    private static final Map<SchemaSpec, Draft> SUPPORTED =
            Map.of(
                    SchemaSpec.DRAFT_04, Draft.DRAFT4,
                    SchemaSpec.DRAFT_07, Draft.DRAFT7,
                    SchemaSpec.DRAFT_2019_09, Draft.DRAFT201909,
                    SchemaSpec.DRAFT_2020_12, Draft.DRAFT202012);

    private static final MetaData METADATA =
            new MetaData(
                    "Vert.x Json Schema",
                    "Vert.x",
                    Language.Java,
                    Licence.Apache_v2_0,
                    SUPPORTED.keySet(),
                    "https://github.com/eclipse-vertx/vertx-json-schema");

    private ObjectMapper mapper = JsonMapper.builder().build();

    @Override
    public MetaData metadata() {
        return METADATA;
    }

    @Override
    public JsonValidator prepare(
            final String schema, final SchemaSpec spec, final AdditionalSchemas additionalSchemas) {
        final Object decodedSchema = Json.decodeValue(schema);

        final JsonSchema parsedSchema =
                decodedSchema instanceof JsonObject
                        ? JsonSchema.of((JsonObject) decodedSchema)
                        : JsonSchema.of((boolean) decodedSchema);

        // Note: doesn't seem to be a way to provide additional schemas
        final Validator validator =
                Validator.create(
                        parsedSchema,
                        new JsonSchemaOptions()
                                .setDraft(schemaVersion(spec))
                                .setBaseUri("https://something.com")
                                .setOutputFormat(OutputFormat.Basic));

        return new JsonValidator() {
            @Override
            public void validate(final String json) {
                final OutputUnit result = validator.validate(Json.decodeValue(json));
                if (!result.getValid()) {
                    throw new RuntimeException(result.toString());
                }
            }

            @Override
            public byte[] serialize(final TestModel model, final boolean validate) {
                try {
                    final Map<String, Object> map =
                            mapper.convertValue(model, new TypeReference<>() {});

                    if (validate) {
                        final OutputUnit result = validator.validate(new JsonObject(map));
                        if (!result.getValid()) {
                            throw new RuntimeException(result.toString());
                        }
                    }

                    return mapper.writeValueAsBytes(map);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public TestModel deserialize(final byte[] data) {
                try {
                    final Map<String, Object> map =
                            mapper.readValue(data, new TypeReference<>() {});

                    final OutputUnit result = validator.validate(new JsonObject(map));
                    if (!result.getValid()) {
                        throw new RuntimeException(result.toString());
                    }

                    return mapper.convertValue(map, TestModel.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private Draft schemaVersion(final SchemaSpec spec) {
        final Draft ver = SUPPORTED.get(spec);
        if (ver == null) {
            throw new IllegalArgumentException("Unsupported: " + spec);
        }
        return ver;
    }
}
