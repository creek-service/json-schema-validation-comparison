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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.gson.JsonElement;
import com.qindesign.json.schema.Error;
import com.qindesign.json.schema.JSON;
import com.qindesign.json.schema.JSONPath;
import com.qindesign.json.schema.MalformedSchemaException;
import com.qindesign.json.schema.Option;
import com.qindesign.json.schema.Options;
import com.qindesign.json.schema.Specification;
import com.qindesign.json.schema.Validator;
import com.qindesign.json.schema.net.URI;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.creekservice.kafka.test.perf.TestSchemas;
import org.creekservice.kafka.test.perf.model.TestModel;
import org.creekservice.kafka.test.perf.testsuite.SchemaSpec;
import org.creekservice.kafka.test.perf.testsuite.ValidatorFactory;

@SuppressWarnings("FieldMayBeFinal") // not final to avoid folding.
public class SnowSerde extends SerdeImpl {

    private Validator validator;
    private ObjectMapper mapper = JsonMapper.builder().build();

    public SnowSerde() {
        this.validator =
                createValidator(TestSchemas.DRAFT_7_SCHEMA, SchemaSpec.DRAFT_07, Optional.empty());
    }

    @Override
    public Serializer serializer() {
        return (model, validate) -> {
            try {
                final byte[] bytes = mapper.writeValueAsBytes(model);

                if (validate) {
                    validate(bytes);
                }

                return bytes;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Override
    public Deserializer deserializer() {
        return bytes -> {
            try {
                validate(bytes);

                return mapper.readValue(bytes, TestModel.class);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    private void validate(final byte[] bytes) throws MalformedSchemaException {
        final JsonElement json = parse(bytes);

        final Map<JSONPath, Map<JSONPath, Error<?>>> errors = new HashMap<>();
        if (!validator.validate(json, new HashMap<>(), errors)) {
            throw new RuntimeException(errors.toString());
        }
    }

    private static JsonElement parse(final byte[] bytes) {
        return JSON.parse(new ByteArrayInputStream(bytes));
    }

    @SuppressWarnings({"CollectionContainsUrl", "OptionalUsedAsFieldOrParameterType"})
    private Validator createValidator(
            final String schema, final SchemaSpec spec, final Optional<Path> remotesDir) {
        try {
            final Map<URI, URL> knownURLs =
                    remotesDir.isPresent()
                            ? Map.of(
                                    URI.parseUnchecked("http://localhost:1234"),
                                    remotesDir.get().toUri().toURL())
                            : Map.of();

            final Options opts = new Options();
            opts.set(Option.FORMAT, true);
            opts.set(Option.CONTENT, true);
            opts.set(Option.DEFAULT_SPECIFICATION, schemaVersion(spec));

            return new Validator(
                    parse(schema.getBytes(UTF_8)),
                    URI.parseUnchecked("https://something.com/"),
                    Map.of(),
                    knownURLs,
                    opts);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final Map<SchemaSpec, Specification> SUPPORTED =
            Map.of(
                    SchemaSpec.DRAFT_06, Specification.DRAFT_06,
                    SchemaSpec.DRAFT_07, Specification.DRAFT_07,
                    SchemaSpec.DRAFT_2019_09, Specification.DRAFT_2019_09);

    private Specification schemaVersion(final SchemaSpec spec) {
        final Specification ver = SUPPORTED.get(spec);
        if (ver == null) {
            throw new IllegalArgumentException("Unsupported: " + spec);
        }
        return ver;
    }

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

                final Validator validator =
                        createValidator(schema, spec, Optional.of(additionalSchemas.remotesDir()));

                return json -> {
                    try {
                        final JsonElement toValidate = parse(json.getBytes(UTF_8));
                        final Map<JSONPath, Map<JSONPath, Error<?>>> errors = new HashMap<>();
                        if (!validator.validate(toValidate, new HashMap<>(), errors)) {
                            throw new RuntimeException(errors.toString());
                        }
                    } catch (MalformedSchemaException e) {
                        throw new RuntimeException(e);
                    }
                };
            }
        };
    }
}
