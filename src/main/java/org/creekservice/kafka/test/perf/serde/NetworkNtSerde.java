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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.creekservice.kafka.test.perf.testsuite.SchemaSpec.DRAFT_04;
import static org.creekservice.kafka.test.perf.testsuite.SchemaSpec.DRAFT_06;
import static org.creekservice.kafka.test.perf.testsuite.SchemaSpec.DRAFT_07;
import static org.creekservice.kafka.test.perf.testsuite.SchemaSpec.DRAFT_2019_09;
import static org.creekservice.kafka.test.perf.testsuite.SchemaSpec.DRAFT_2020_12;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.networknt.schema.JsonMetaSchema;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.creekservice.kafka.test.perf.TestSchemas;
import org.creekservice.kafka.test.perf.model.TestModel;
import org.creekservice.kafka.test.perf.testsuite.SchemaSpec;
import org.creekservice.kafka.test.perf.testsuite.ValidatorFactory;

@SuppressWarnings("FieldMayBeFinal") // not final to avoid folding.
public class NetworkNtSerde extends SerdeImpl {

    private JsonSchema schema;
    private ObjectMapper mapper = JsonMapper.builder().build();

    public NetworkNtSerde() {
        schema =
                JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
                        .getSchema(TestSchemas.DRAFT_7_SCHEMA);
    }

    @Override
    public Serializer serializer() {
        return (model, validate) -> {
            try {
                final JsonNode node = mapper.convertValue(model, JsonNode.class);

                if (validate) {
                    final Set<ValidationMessage> errors = schema.validate(node);
                    if (!errors.isEmpty()) {
                        throw new RuntimeException(errors.toString());
                    }
                }

                return mapper.writeValueAsBytes(node);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Override
    public Deserializer deserializer() {
        return bytes -> {
            try {
                final JsonNode node = mapper.readValue(bytes, JsonNode.class);

                final Set<ValidationMessage> errors = schema.validate(node);
                if (!errors.isEmpty()) {
                    throw new RuntimeException(errors.toString());
                }

                return mapper.convertValue(node, TestModel.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private static final Map<SchemaSpec, SpecVersion.VersionFlag> SUPPORTED =
            Map.of(
                    DRAFT_04, SpecVersion.VersionFlag.V4,
                    DRAFT_06, SpecVersion.VersionFlag.V6,
                    DRAFT_07, SpecVersion.VersionFlag.V7,
                    DRAFT_2019_09, SpecVersion.VersionFlag.V201909,
                    DRAFT_2020_12, SpecVersion.VersionFlag.V202012);

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

                final JsonSchema parsedSchema = getParsedSchema(schema, spec, additionalSchemas);

                return json -> {
                    try {
                        final JsonNode node = mapper.readValue(json, JsonNode.class);

                        final Set<ValidationMessage> errors = parsedSchema.validate(node);
                        if (!errors.isEmpty()) {
                            throw new RuntimeException(errors.toString());
                        }
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                };
            }

            private JsonSchema getParsedSchema(
                    final String schema,
                    final SchemaSpec spec,
                    final AdditionalSchemas additionalSchemas) {
                final JsonMetaSchema metaSchema =
                        JsonSchemaFactory.checkVersion(schemaVersion(spec)).getInstance();

                return JsonSchemaFactory.builder()
                        .defaultMetaSchemaURI(metaSchema.getUri())
                        .addMetaSchema(metaSchema)
                        .uriFetcher(
                                uri ->
                                        new ByteArrayInputStream(
                                                additionalSchemas.load(uri).getBytes(UTF_8)),
                                Set.of("http", "https"))
                        .build()
                        .getSchema(schema);
            }

            private SpecVersion.VersionFlag schemaVersion(final SchemaSpec spec) {
                final SpecVersion.VersionFlag ver = SUPPORTED.get(spec);
                if (ver == null) {
                    throw new IllegalArgumentException("Unsupported: " + spec);
                }
                return ver;
            }
        };
    }
}
