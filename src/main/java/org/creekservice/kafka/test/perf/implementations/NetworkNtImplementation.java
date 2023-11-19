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
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.creekservice.kafka.test.perf.model.TestModel;
import org.creekservice.kafka.test.perf.testsuite.AdditionalSchemas;
import org.creekservice.kafka.test.perf.testsuite.SchemaSpec;

@SuppressWarnings("FieldMayBeFinal") // not final to avoid folding.
public class NetworkNtImplementation implements Implementation {

    private static final Map<SchemaSpec, SpecVersion.VersionFlag> SUPPORTED =
            Map.of(
                    DRAFT_04, SpecVersion.VersionFlag.V4,
                    DRAFT_06, SpecVersion.VersionFlag.V6,
                    DRAFT_07, SpecVersion.VersionFlag.V7,
                    DRAFT_2019_09, SpecVersion.VersionFlag.V201909,
                    DRAFT_2020_12, SpecVersion.VersionFlag.V202012);

    private static final MetaData METADATA =
            new MetaData(
                    "networknt/json-schema-validator",
                    "NetworkNt",
                    Language.Java,
                    Licence.Apache_v2_0,
                    SUPPORTED.keySet(),
                    "https://github.com/networknt/json-schema-validator",
                    new Color(255, 205, 86),
                    com.networknt.schema.JsonSchemaFactory.class,
                    MetaData.ACTIVE_PROJECT);

    private ObjectMapper mapper = JsonMapper.builder().build();

    @Override
    public MetaData metadata() {
        return METADATA;
    }

    @Override
    public JsonValidator prepare(
            final String schema,
            final SchemaSpec spec,
            final AdditionalSchemas additionalSchemas,
            final boolean enableFormatAssertions) {

        /*
        Implementation does not seem to currently provide a way to programmatically turn on format assertions.
        Instead, they seem to be on-by-default, which is not inline with the draft 2020-12 spec.
         */

        final JsonSchema parsedSchema = parseSchema(schema, spec, additionalSchemas);

        return new JsonValidator() {
            @Override
            public void validate(final String json) {
                try {
                    parse(json.getBytes(UTF_8));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public byte[] serialize(final TestModel model, final boolean validate) {
                try {
                    final JsonNode node = mapper.convertValue(model, JsonNode.class);

                    if (validate) {
                        final Set<ValidationMessage> errors = parsedSchema.validate(node);
                        if (!errors.isEmpty()) {
                            throw new RuntimeException(errors.toString());
                        }
                    }

                    return mapper.writeValueAsBytes(node);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public TestModel deserialize(final byte[] data) {
                try {
                    final JsonNode node = parse(data);
                    return mapper.convertValue(node, TestModel.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            private JsonNode parse(final byte[] data) throws IOException {
                final JsonNode node = mapper.readValue(data, JsonNode.class);

                final Set<ValidationMessage> errors = parsedSchema.validate(node);
                if (!errors.isEmpty()) {
                    throw new RuntimeException(errors.toString());
                }
                return node;
            }
        };
    }

    private JsonSchema parseSchema(
            final String schema, final SchemaSpec spec, final AdditionalSchemas additionalSchemas) {
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
}
