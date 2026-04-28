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
import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaRegistryConfig;
import com.networknt.schema.SpecificationVersion;
import com.networknt.schema.regex.JoniRegularExpressionFactory;
import java.awt.Color;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.creekservice.kafka.test.perf.model.TestModel;
import org.creekservice.kafka.test.perf.testsuite.AdditionalSchemas;
import org.creekservice.kafka.test.perf.testsuite.SchemaSpec;

@SuppressWarnings("FieldMayBeFinal") // not final to avoid folding.
public class NetworkNtImplementation implements Implementation {

    private static final Map<SchemaSpec, SpecificationVersion> SUPPORTED =
            Map.of(
                    DRAFT_04, SpecificationVersion.DRAFT_4,
                    DRAFT_06, SpecificationVersion.DRAFT_6,
                    DRAFT_07, SpecificationVersion.DRAFT_7,
                    DRAFT_2019_09, SpecificationVersion.DRAFT_2019_09,
                    DRAFT_2020_12, SpecificationVersion.DRAFT_2020_12);

    private static final MetaData METADATA =
            new MetaData(
                    "networknt/json-schema-validator",
                    "NetworkNt",
                    Language.Java,
                    Licence.Apache_v2_0,
                    SUPPORTED.keySet(),
                    "https://github.com/networknt/json-schema-validator",
                    new Color(255, 205, 86),
                    com.networknt.schema.SchemaRegistry.class,
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

        final Schema parsedSchema = parseSchema(schema, spec, additionalSchemas);

        return new JsonValidator() {
            @Override
            public void validate(final String json) {
                doValidate(json);
            }

            @Override
            public byte[] serialize(final TestModel model, final boolean validate) {
                try {
                    final byte[] bytes = mapper.writeValueAsBytes(model);

                    if (validate) {
                        doValidate(new String(bytes, UTF_8));
                    }

                    return bytes;
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
                doValidate(new String(data, UTF_8));
                return mapper.readValue(data, JsonNode.class);
            }

            private void doValidate(final String json) {
                final List<Error> errors =
                        parsedSchema.validate(
                                json,
                                InputFormat.JSON,
                                executionContext ->
                                        executionContext.executionConfig(
                                                config ->
                                                        config.formatAssertionsEnabled(
                                                                enableFormatAssertions
                                                                        ? true
                                                                        : null)));
                if (!errors.isEmpty()) {
                    throw new RuntimeException(errors.toString());
                }
            }
        };
    }

    private Schema parseSchema(
            final String schema, final SchemaSpec spec, final AdditionalSchemas additionalSchemas) {
        // By default, the library uses the JDK regular expression implementation which is not ECMA
        // 262 compliant. This requires the joni dependency
        final SchemaRegistryConfig config =
                SchemaRegistryConfig.builder()
                        .regularExpressionFactory(JoniRegularExpressionFactory.getInstance())
                        .build();
        return SchemaRegistry.withDefaultDialect(
                        schemaVersion(spec),
                        builder ->
                                builder.schemas(additionalSchemas::load)
                                        .schemaRegistryConfig(config))
                .getSchema(schema);
    }

    private SpecificationVersion schemaVersion(final SchemaSpec spec) {
        final SpecificationVersion ver = SUPPORTED.get(spec);
        if (ver == null) {
            throw new IllegalArgumentException("Unsupported: " + spec);
        }
        return ver;
    }
}
