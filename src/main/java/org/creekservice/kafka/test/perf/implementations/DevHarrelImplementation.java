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

import static org.creekservice.kafka.test.perf.testsuite.SchemaSpec.DRAFT_2019_09;
import static org.creekservice.kafka.test.perf.testsuite.SchemaSpec.DRAFT_2020_12;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import dev.harrel.jsonschema.Dialects;
import dev.harrel.jsonschema.SpecificationVersion;
import dev.harrel.jsonschema.Validator;
import java.awt.Color;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.creekservice.kafka.test.perf.model.TestModel;
import org.creekservice.kafka.test.perf.testsuite.AdditionalSchemas;
import org.creekservice.kafka.test.perf.testsuite.SchemaSpec;

@SuppressWarnings("FieldMayBeFinal") // not final to avoid folding.
public class DevHarrelImplementation implements Implementation {

    private static final MetaData METADATA =
            new MetaData(
                    "json-schema (dev.harrel)",
                    "dev.harrel",
                    Language.Java,
                    Licence.MIT,
                    Set.of(DRAFT_2020_12, DRAFT_2019_09),
                    "https://github.com/harrel56/json-schema",
                    new Color(235, 54, 172));

    private ObjectMapper mapper = JsonMapper.builder().build();

    @Override
    public MetaData metadata() {
        return METADATA;
    }

    @Override
    public JsonValidator prepare(
            final String schema, final SchemaSpec spec, final AdditionalSchemas additionalSchemas) {

        final Validator validator = validator(spec);
        /* Parse remotes eagerly and ignore errors from unknown specs */
        additionalSchemas
                .remotes()
                .forEach(
                        (uri, remote) -> {
                            try {
                                validator.registerSchema(uri, remote);
                            } catch (Exception e) {
                                /* ignore */
                            }
                        });
        final URI schemaUri = validator.registerSchema(schema);

        return new JsonValidator() {

            @Override
            public void validate(final String json) {
                final Validator.Result result = validator.validate(schemaUri, json);
                if (!result.isValid()) {
                    throw new RuntimeException();
                }
            }

            @Override
            public byte[] serialize(final TestModel model, final boolean validate) {
                try {
                    final String asString = mapper.writeValueAsString(model);
                    if (validate && !validator.validate(schemaUri, asString).isValid()) {
                        throw new RuntimeException();
                    }
                    return asString.getBytes(StandardCharsets.UTF_8);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public TestModel deserialize(final byte[] data) {
                try {
                    final String json = new String(data, StandardCharsets.UTF_8);
                    if (!validator.validate(schemaUri, json).isValid()) {
                        throw new RuntimeException();
                    }
                    return mapper.readValue(data, TestModel.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private Validator validator(final SchemaSpec spec) {
        switch (spec) {
            case DRAFT_2020_12:
                final Validator validator2020 =
                        new dev.harrel.jsonschema.ValidatorFactory()
                                .withDialect(new Dialects.Draft2020Dialect())
                                .createValidator();
                /* Validate against meta-schema in order to parse it eagerly */
                validator2020.validate(URI.create(SpecificationVersion.DRAFT2020_12.getId()), "{}");
                return validator2020;
            case DRAFT_2019_09:
                final Validator validator2019 =
                        new dev.harrel.jsonschema.ValidatorFactory()
                                .withDialect(new Dialects.Draft2019Dialect())
                                .createValidator();
                /* Validate against meta-schema in order to parse it eagerly */
                validator2019.validate(URI.create(SpecificationVersion.DRAFT2019_09.getId()), "{}");
                return validator2019;
            default:
                throw new RuntimeException("Unsupported Spec:" + spec);
        }
    }

    // Final, empty finalize method stops spotbugs CT_CONSTRUCTOR_THROW
    // Can be moved to base type after https://github.com/spotbugs/spotbugs/issues/2665
    @Override
    @SuppressWarnings({"deprecation", "Finalize"})
    protected final void finalize() {}
}
