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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import dev.harrel.jsonschema.Dialects;
import dev.harrel.jsonschema.SchemaResolver;
import dev.harrel.jsonschema.SpecificationVersion;
import dev.harrel.jsonschema.Validator;
import org.creekservice.kafka.test.perf.TestSchemas;
import org.creekservice.kafka.test.perf.model.TestModel;
import org.creekservice.kafka.test.perf.testsuite.AdditionalSchemas;
import org.creekservice.kafka.test.perf.testsuite.SchemaSpec;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import static org.creekservice.kafka.test.perf.testsuite.SchemaSpec.*;

@SuppressWarnings("FieldMayBeFinal") // not final to avoid folding.
public class DevHarrelImplementation implements Implementation {

    private static final MetaData METADATA =
            new MetaData(
                    "json-schema (dev.harrel)",
                    "json-schema (dev.harrel)",
                    Language.Java,
                    Licence.MIT,
                    Set.of(DRAFT_2020_12, DRAFT_2019_09),
                    "https://github.com/harrel56/json-schema");

    private final ObjectMapper mapper = JsonMapper.builder().build();
    private final Map<SchemaSpec, Validator> validators;
    private final URI testSchemaUri = URI.create("urn:test");
    private Map<URI, String> remotes = Map.of();

    public DevHarrelImplementation() {
        SchemaResolver schemaResolver = uri -> {
            String resolved = remotes.get(URI.create(uri));
            if (resolved == null) {
                return SchemaResolver.Result.empty();
            }
            return SchemaResolver.Result.fromString(resolved);
        };
        Validator validator2020 = new dev.harrel.jsonschema.ValidatorFactory()
                .withSchemaResolver(schemaResolver)
                .createValidator();
        Validator validator2019 = new dev.harrel.jsonschema.ValidatorFactory()
                .withDialect(new Dialects.Draft2019Dialect())
                .withSchemaResolver(schemaResolver)
                .createValidator();
        /* Validate against meta-schemas in order to parse them eagerly */
        validator2020.validate(URI.create(SpecificationVersion.DRAFT2020_12.getId()), "{}");
        validator2019.validate(URI.create(SpecificationVersion.DRAFT2019_09.getId()), "{}");

        validator2020.registerSchema(testSchemaUri, TestSchemas.DRAFT_2020_SCHEMA);

        this.validators = Map.of(
                DRAFT_2020_12, validator2020,
                DRAFT_2019_09, validator2019);
    }

    @Override
    public MetaData metadata() {
        return METADATA;
    }

    @Override
    public JsonValidator prepare(
            final String schema, final SchemaSpec spec, final AdditionalSchemas additionalSchemas) {
        DevHarrelImplementation.this.remotes = additionalSchemas.remotes();
        Validator validator = validators.get(spec);
        URI schemaUri = validator.registerSchema(schema);

        return new JsonValidator() {
            @Override
            public void validate(final String json) {
                Validator.Result result = validator.validate(schemaUri, json);
                if (!result.isValid()) {
                    throw new RuntimeException();
                }
            }

            @Override
            public byte[] serialize(final TestModel model, final boolean validate) {
                try {
                    String asString = mapper.writeValueAsString(model);
                    if (validate) {
                        validators.get(DRAFT_2020_12).validate(testSchemaUri, asString);
                    }
                    return asString.getBytes(StandardCharsets.UTF_8);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public TestModel deserialize(final byte[] data) {
                try {
                    validators.get(DRAFT_2020_12).validate(testSchemaUri, new String(data, StandardCharsets.UTF_8));
                    return mapper.readValue(data, TestModel.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    // Final, empty finalize method stops spotbugs CT_CONSTRUCTOR_THROW
    // Can be moved to base type after https://github.com/spotbugs/spotbugs/issues/2665
    @Override
    @SuppressWarnings({"deprecation", "Finalize"})
    protected final void finalize() {}
}
