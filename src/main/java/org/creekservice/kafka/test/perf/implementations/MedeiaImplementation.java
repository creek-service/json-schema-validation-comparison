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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.worldturner.medeia.api.JsonSchemaVersion;
import com.worldturner.medeia.api.MetaSchemaSource;
import com.worldturner.medeia.api.SchemaSource;
import com.worldturner.medeia.api.StringSchemaSource;
import com.worldturner.medeia.api.ValidationOptions;
import com.worldturner.medeia.api.jackson.MedeiaJacksonApi;
import com.worldturner.medeia.schema.validation.SchemaValidator;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.creekservice.kafka.test.perf.model.TestModel;
import org.creekservice.kafka.test.perf.testsuite.AdditionalSchemas;
import org.creekservice.kafka.test.perf.testsuite.SchemaSpec;
import org.creekservice.kafka.test.perf.util.TestSchemas;

@SuppressWarnings("FieldMayBeFinal") // not final to avoid folding.
public class MedeiaImplementation implements Implementation {

    private static final Map<SchemaSpec, JsonSchemaVersion> SUPPORTED =
            Map.of(
                    DRAFT_04, JsonSchemaVersion.DRAFT04,
                    DRAFT_06, JsonSchemaVersion.DRAFT06,
                    DRAFT_07, JsonSchemaVersion.DRAFT07);

    private static final MetaData METADATA =
            new MetaData(
                    "worldturner/medeia-validator",
                    "Medeia",
                    Language.Kotlin,
                    Licence.Apache_v2_0,
                    SUPPORTED.keySet(),
                    "https://github.com/worldturner/medeia-validator",
                    new Color(201, 203, 207),
                    com.worldturner.medeia.schema.validation.SchemaValidator.class,
                    "No sign of active development - Last released Jun, 2019.");

    private static final ValidationOptions VALIDATOR_OPTIONS =
            new ValidationOptions().withValidateSchema(false);

    private ObjectMapper mapper = JsonMapper.builder().build();
    private MedeiaJacksonApi api = new MedeiaJacksonApi();
    private SchemaValidator schemaValidator =
            api.loadSchemas(
                    List.of(new StringSchemaSource(TestSchemas.DRAFT_7_SCHEMA)), VALIDATOR_OPTIONS);

    @Override
    public MetaData metadata() {
        return METADATA;
    }

    @Override
    public JsonValidator prepare(
            final String schema, final SchemaSpec spec, final AdditionalSchemas additionalSchemas) {
        final JsonSchemaVersion version = schemaVersion(spec);

        // Doesn't seem to be a way to reactively 'load' schema on demand:
        // Only to provide them all, which means they ALL get parsed... slow!
        final List<SchemaSource> schemas =
                new ArrayList<>(additionalSchema(additionalSchemas, version));
        schemas.add(MetaSchemaSource.Companion.forVersion(version));
        schemas.add(0, new StringSchemaSource(schema, version));

        final SchemaValidator v = api.loadSchemas(schemas, VALIDATOR_OPTIONS);

        return new JsonValidator() {
            @Override
            public void validate(final String json) {
                parseAndValidate(json.getBytes(StandardCharsets.UTF_8), JsonNode.class);
            }

            @Override
            public byte[] serialize(final TestModel model, final boolean validate) {
                try {
                    final ByteArrayOutputStream out = new ByteArrayOutputStream();
                    JsonGenerator generator = mapper.getFactory().createGenerator(out);
                    if (validate) {
                        generator = api.decorateJsonGenerator(schemaValidator, generator);
                    }
                    mapper.writeValue(generator, model);
                    return out.toByteArray();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public TestModel deserialize(final byte[] data) {
                return parseAndValidate(data, TestModel.class);
            }

            private <T> T parseAndValidate(final byte[] data, final Class<T> type) {
                try {
                    final JsonParser parser = mapper.createParser(data);
                    final JsonParser validatingParser = api.decorateJsonParser(v, parser);
                    return mapper.reader().readValue(validatingParser, type);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private List<StringSchemaSource> additionalSchema(
            final AdditionalSchemas additionalSchemas, final JsonSchemaVersion version) {
        return additionalSchemas.remotes().entrySet().stream()
                .filter(
                        e ->
                                !e.getKey().getPath().startsWith("/draft")
                                        || e.getKey().getPath().startsWith("/draft7"))
                .filter(e -> !e.getKey().getPath().endsWith("nested-absolute-ref-to-string.json"))
                .map(e -> new StringSchemaSource(e.getValue(), version, e.getKey()))
                .collect(Collectors.toList());
    }

    private JsonSchemaVersion schemaVersion(final SchemaSpec spec) {
        final JsonSchemaVersion ver = SUPPORTED.get(spec);
        if (ver == null) {
            throw new IllegalArgumentException("Unsupported: " + spec);
        }
        return ver;
    }
}
