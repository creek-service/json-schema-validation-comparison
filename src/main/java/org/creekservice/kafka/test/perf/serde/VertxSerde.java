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

package org.creekservice.kafka.test.perf.serde;

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
import java.util.Set;
import org.creekservice.kafka.test.perf.TestSchemas;
import org.creekservice.kafka.test.perf.model.TestModel;
import org.creekservice.kafka.test.perf.testsuite.SchemaSpec;
import org.creekservice.kafka.test.perf.testsuite.ValidatorFactory;

@SuppressWarnings("FieldMayBeFinal") // not final to avoid folding.
public class VertxSerde extends SerdeImpl {

    private ObjectMapper mapper = JsonMapper.builder().build();
    private Validator validator;

    public VertxSerde() {
        this.validator =
                Validator.create(
                        JsonSchema.of(new JsonObject(TestSchemas.DRAFT_7_SCHEMA)),
                        new JsonSchemaOptions()
                                .setDraft(Draft.DRAFT7)
                                .setBaseUri("https://something.io")
                                .setOutputFormat(OutputFormat.Basic));
    }

    @Override
    public Serializer serializer() {
        return (model, validate) -> {
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
        };
    }

    @Override
    public Deserializer deserializer() {
        return bytes -> {
            try {
                final Map<String, Object> map = mapper.readValue(bytes, new TypeReference<>() {});

                final OutputUnit result = validator.validate(new JsonObject(map));
                if (!result.getValid()) {
                    throw new RuntimeException(result.toString());
                }

                return mapper.convertValue(map, TestModel.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private static final Map<SchemaSpec, Draft> SUPPORTED =
            Map.of(
                    SchemaSpec.DRAFT_04, Draft.DRAFT4,
                    SchemaSpec.DRAFT_07, Draft.DRAFT7,
                    SchemaSpec.DRAFT_2019_09, Draft.DRAFT201909,
                    SchemaSpec.DRAFT_2020_12, Draft.DRAFT202012);

    @Override
    public ValidatorFactory validator() {
        return new ValidatorFactory() {
            @Override
            public Set<SchemaSpec> supports() {
                return SUPPORTED.keySet();
            }

            @Override
            public JsonValidator prepare(
                    final String schema,
                    final SchemaSpec spec,
                    final AdditionalSchemas additionalSchemas) {
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

                return json -> {
                    final OutputUnit result = validator.validate(Json.decodeValue(json));
                    if (!result.getValid()) {
                        throw new RuntimeException(result.toString());
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
        };
    }
}
