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

import static org.creekservice.kafka.test.perf.testsuite.SchemaSpec.DRAFT_04;
import static org.creekservice.kafka.test.perf.testsuite.SchemaSpec.DRAFT_06;
import static org.creekservice.kafka.test.perf.testsuite.SchemaSpec.DRAFT_07;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.json.JsonReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import org.creekservice.kafka.test.perf.TestSchemas;
import org.creekservice.kafka.test.perf.model.TestModel;
import org.creekservice.kafka.test.perf.testsuite.SchemaSpec;
import org.creekservice.kafka.test.perf.testsuite.ValidatorFactory;
import org.leadpony.justify.api.JsonSchema;
import org.leadpony.justify.api.JsonSchemaResolver;
import org.leadpony.justify.api.JsonValidationService;
import org.leadpony.justify.api.ProblemHandler;
import org.leadpony.justify.api.SpecVersion;

@SuppressWarnings("FieldMayBeFinal") // not final to avoid folding.
public class JustifySerde extends SerdeImpl {

    private JsonValidationService service;
    private JsonSchema schema;
    private ProblemHandler handler;
    private ObjectMapper mapper = JsonMapper.builder().build();

    public JustifySerde() {
        service = JsonValidationService.newInstance();
        schema =
                service.readSchema(
                        new ByteArrayInputStream(
                                TestSchemas.DRAFT_7_SCHEMA.getBytes(StandardCharsets.UTF_8)));
        handler =
                problems -> {
                    throw new RuntimeException(problems.toString());
                };
    }

    @Override
    public Serializer serializer() {
        return (model, validate) -> {
            try {
                final byte[] bytes = mapper.writeValueAsBytes(model);

                if (validate) {
                    // Double parse seems unavoidable, even if using json-b:
                    try (JsonReader reader =
                            service.createReader(
                                    new ByteArrayInputStream(bytes), schema, handler)) {
                        reader.readValue();
                    }
                }

                return bytes;
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Override
    public Deserializer deserializer() {
        return bytes -> {
            try {
                try (JsonReader reader =
                        service.createReader(new ByteArrayInputStream(bytes), schema, handler)) {
                    reader.readValue();
                }

                // Double parse seems unavoidable, even if using json-b:
                return mapper.readValue(bytes, TestModel.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private static final Map<SchemaSpec, SpecVersion> SUPPORTED =
            Map.of(
                    DRAFT_04, SpecVersion.DRAFT_04,
                    DRAFT_06, SpecVersion.DRAFT_06,
                    DRAFT_07, SpecVersion.DRAFT_07);

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

                final JsonSchema parsedSchema = parseSchema(schema, spec, additionalSchemas);

                return json -> {
                    try (JsonReader reader =
                            service.createReader(
                                    new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
                                    parsedSchema,
                                    handler)) {
                        reader.readValue();
                    }
                };
            }

            private JsonSchema parseSchema(
                    final String schema,
                    final SchemaSpec spec,
                    final AdditionalSchemas additionalSchemas) {
                final JsonSchemaResolver resolver =
                        uri -> {
                            final String s = additionalSchemas.load(uri);
                            return parseSchema(s, spec, additionalSchemas);
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
        };
    }

    // Final, empty finalize method stops spotbugs CT_CONSTRUCTOR_THROW
    // Can be moved to base type after https://github.com/spotbugs/spotbugs/issues/2665
    @Override
    @SuppressWarnings({"deprecation", "Finalize"})
    protected final void finalize() {}
}
