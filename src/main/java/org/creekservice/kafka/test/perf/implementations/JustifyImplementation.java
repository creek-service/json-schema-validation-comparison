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

import static org.creekservice.kafka.test.perf.testsuite.SchemaSpec.DRAFT_04;
import static org.creekservice.kafka.test.perf.testsuite.SchemaSpec.DRAFT_06;
import static org.creekservice.kafka.test.perf.testsuite.SchemaSpec.DRAFT_07;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.json.JsonReader;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.creekservice.kafka.test.perf.model.TestModel;
import org.creekservice.kafka.test.perf.testsuite.AdditionalSchemas;
import org.creekservice.kafka.test.perf.testsuite.SchemaSpec;
import org.leadpony.justify.api.JsonSchema;
import org.leadpony.justify.api.JsonSchemaResolver;
import org.leadpony.justify.api.JsonValidationService;
import org.leadpony.justify.api.ProblemHandler;
import org.leadpony.justify.api.SpecVersion;

@SuppressWarnings("FieldMayBeFinal") // not final to avoid folding.
public class JustifyImplementation implements Implementation {

    private static final Map<SchemaSpec, SpecVersion> SUPPORTED =
            Map.of(
                    DRAFT_04, SpecVersion.DRAFT_04,
                    DRAFT_06, SpecVersion.DRAFT_06,
                    DRAFT_07, SpecVersion.DRAFT_07);

    private static final MetaData METADATA =
            new MetaData(
                    "Justify",
                    "Justify",
                    Language.Java,
                    Licence.Apache_v2_0,
                    SUPPORTED.keySet(),
                    "https://github.com/leadpony/justify",
                    new Color(153, 102, 255));

    private ProblemHandler handler =
            problems -> {
                throw new RuntimeException(problems.toString());
            };
    private ObjectMapper mapper = JsonMapper.builder().build();

    @Override
    public MetaData metadata() {
        return METADATA;
    }

    @Override
    public JsonValidator prepare(
            final String schema, final SchemaSpec spec, final AdditionalSchemas additionalSchemas) {

        final JsonValidationService service = JsonValidationService.newInstance();
        final JsonSchema parsedSchema = parseSchema(service, schema, spec, additionalSchemas);
        return new JsonValidator() {
            @Override
            public void validate(final String json) {
                validate(service, json.getBytes(StandardCharsets.UTF_8), parsedSchema);
            }

            @Override
            public byte[] serialize(final TestModel model, final boolean validate) {
                try {
                    final byte[] bytes = mapper.writeValueAsBytes(model);

                    if (validate) {
                        // Double parse seems unavoidable, even if using json-b:
                        validate(service, bytes, parsedSchema);
                    }

                    return bytes;
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public TestModel deserialize(final byte[] data) {
                try {
                    validate(service, data, parsedSchema);

                    // Double parse seems unavoidable, even if using json-b:
                    return mapper.readValue(data, TestModel.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            private void validate(
                    final JsonValidationService service,
                    final byte[] json,
                    final JsonSchema parsedSchema) {
                try (JsonReader reader =
                        service.createReader(
                                new ByteArrayInputStream(json), parsedSchema, handler)) {
                    reader.readValue();
                }
            }
        };
    }

    private JsonSchema parseSchema(
            final JsonValidationService service,
            final String schema,
            final SchemaSpec spec,
            final AdditionalSchemas additionalSchemas) {
        final JsonSchemaResolver resolver =
                uri -> {
                    final String s = additionalSchemas.load(uri);
                    return parseSchema(service, s, spec, additionalSchemas);
                };

        return service.createSchemaReaderFactoryBuilder()
                .withDefaultSpecVersion(schemaVersion(spec))
                .withSchemaResolver(resolver)
                .withSchemaValidation(false)
                .build()
                .createSchemaReader(
                        new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8)))
                .read();
    }

    private SpecVersion schemaVersion(final SchemaSpec spec) {
        final SpecVersion ver = SUPPORTED.get(spec);
        if (ver == null) {
            throw new IllegalArgumentException("Unsupported: " + spec);
        }
        return ver;
    }
}
