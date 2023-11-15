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

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.awt.Color;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.creekservice.kafka.test.perf.model.TestModel;
import org.creekservice.kafka.test.perf.testsuite.AdditionalSchemas;
import org.creekservice.kafka.test.perf.testsuite.SchemaSpec;

public interface Implementation {

    interface JsonValidator {
        /**
         * Validate a JSON document.
         *
         * @param json the JSON to validate
         */
        void validate(String json);

        /**
         * Serialize the model to bytes
         *
         * @param model the model to serialize
         * @param validate flag indicating if JSON should be validated against the schema. Setting
         *     this to {@code false} allows the serializer to generate invalid JSON, which allows
         *     testing of negative path in the deserializer.
         * @return the serialized bytes.
         */
        byte[] serialize(TestModel model, boolean validate);

        /**
         * Deserialise a model from the supplied bytes.
         *
         * <p>Implementations should <i>always</i> validate the supplied JSON.
         *
         * @param data the bytes containing the JSON document to deserialize.
         * @return the deserialized object.
         */
        TestModel deserialize(byte[] data);
    }

    enum Language {
        Java,
        Kotlin
    }

    enum Licence {
        Apache_v2_0("Apache Licence 2.0"),
        MIT("MIT"),
        GNU_Affero_General_Public_v3_0("GNU Affero General Public License v3.0");

        private final String text;

        Licence(final String text) {
            this.text = requireNonNull(text, "test");
        }

        @JsonValue
        @Override
        public String toString() {
            return text;
        }
    }

    class MetaData {

        public static final Pattern SHORT_NAME_PATTERN = Pattern.compile("[A-Za-z0-9]+");

        private final String longName;
        private final String shortName;
        private final Language language;
        private final Licence licence;
        private final Set<SchemaSpec> supported;
        private final URL url;
        private final Color color;

        /**
         * Construct metadata about a specific validator implementation.
         *
         * @param longName a more expressive name.
         * @param shortName the short name, as used in reports. Can only contain alphanumeric
         *     characters.
         * @param language the programming language the validator library is written in.
         * @param licence the licence the validator library is released under.
         * @param supported the set of supported JSON schema draft specifications.
         * @param url the url to the validator libraries implementation or documentation.
         * @param color the RGB color to use for this implementation in <a
         *     href="https://www.creekservice.org/json-schema-validation-comparison/functional#summary-of-results">charts</a>.
         *     Alpha is ignored.
         */
        public MetaData(
                final String longName,
                final String shortName,
                final Language language,
                final Licence licence,
                final Set<SchemaSpec> supported,
                final String url,
                final Color color) {
            this.longName = requireNonNull(longName, "longName").trim();
            this.shortName = requireNonNull(shortName, "shortName").trim();
            this.language = requireNonNull(language, "language");
            this.licence = requireNonNull(licence, "licence");
            this.supported = Set.copyOf(requireNonNull(supported, "supported"));
            this.color = requireNonNull(color, "color");
            try {
                this.url = new URL(requireNonNull(url, "url"));
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }

            if (longName.isBlank()) {
                throw new IllegalArgumentException("Long name blank");
            }

            if (shortName.isBlank()) {
                throw new IllegalArgumentException("Short name blank");
            }

            if (!SHORT_NAME_PATTERN.matcher(shortName).matches()) {
                throw new IllegalArgumentException(
                        "Short name not match required pattern: " + SHORT_NAME_PATTERN.pattern());
            }
        }

        @JsonProperty("longName")
        public String longName() {
            return longName;
        }

        @JsonProperty("shortName")
        public String shortName() {
            return shortName;
        }

        @JsonProperty("language")
        public Language language() {
            return language;
        }

        @JsonProperty("licence")
        public Licence licence() {
            return licence;
        }

        @JsonProperty("url")
        public URL url() {
            return url;
        }

        @JsonProperty("supported")
        public Set<SchemaSpec> supported() {
            return new TreeSet<>(supported);
        }

        @JsonProperty("color")
        public String color() {
            return "rgb(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ")";
        }
    }

    /**
     * @return metadata about the implementation
     */
    MetaData metadata();

    /**
     * Test if impl supports the supplied {@code spec}.
     *
     * @param spec the spec to test.
     * @return {@code true} if the impl supports the supplied {@code spec}.
     */
    default boolean supports(final SchemaSpec spec) {
        return metadata().supported().contains(spec);
    }

    /**
     * Prepare a {@link JsonValidator} that is capable of validating JSON conforming to the supplied
     * {@code schema}.
     *
     * <p>Implementations should do as much work upfront as possible, e.g. parsing schemas and
     * building validators, as the time spent in this method does not currently form part of the
     * benchmarked code, i.e. time spent in this method is ignored in the benchmarking.
     *
     * @param schema the schema to validate with
     * @param spec the spec of the schema
     * @param additionalSchemas accessor to meta-schemas and JSON-Schema-Test-Suite 'remote'
     *     schemas.
     * @return a validator instance that be can be used to validate, serialise and deserialise JSON.
     */
    JsonValidator prepare(String schema, SchemaSpec spec, AdditionalSchemas additionalSchemas);
}
