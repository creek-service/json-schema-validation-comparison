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

package org.creekservice.kafka.test.perf.testsuite;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public enum SchemaSpec {
    DRAFT_03("draft3", "http://json-schema.org/draft-03/schema#", Set.of()),
    DRAFT_04("draft4", "http://json-schema.org/draft-04/schema#", Set.of()),
    DRAFT_06("draft6", "http://json-schema.org/draft-06/schema#", Set.of()),
    DRAFT_07("draft7", "http://json-schema.org/draft-07/schema#", Set.of()),
    DRAFT_2019_09(
            "draft2019-09",
            "https://json-schema.org/draft/2019-09/schema",
            Set.of(
                    "https://json-schema.org/draft/2019-09/meta/validation",
                    "https://json-schema.org/draft/2019-09/meta/core",
                    "https://json-schema.org/draft/2019-09/meta/applicator",
                    "https://json-schema.org/draft/2019-09/meta/meta-data",
                    "https://json-schema.org/draft/2019-09/meta/format",
                    "https://json-schema.org/draft/2019-09/meta/content")),
    DRAFT_2020_12(
            "draft2020-12",
            "https://json-schema.org/draft/2020-12/schema",
            Set.of(
                    "https://json-schema.org/draft/2020-12/meta/validation",
                    "https://json-schema.org/draft/2020-12/meta/core",
                    "https://json-schema.org/draft/2020-12/meta/applicator",
                    "https://json-schema.org/draft/2020-12/meta/meta-data",
                    "https://json-schema.org/draft/2020-12/meta/content",
                    "https://json-schema.org/draft/2020-12/meta/format-annotation",
                    "https://json-schema.org/draft/2020-12/meta/unevaluated"));

    private final String dirName;
    private final URI uri;
    private final String content;
    private final Map<URI, String> additional;

    SchemaSpec(final String dirName, final String uri, final Set<String> additional) {
        this.dirName = requireNonNull(dirName, "dirName");
        this.uri = URI.create(uri);
        this.content = loadContent(this.uri);
        this.additional =
                additional.stream()
                        .map(URI::create)
                        .collect(toMap(Function.identity(), SchemaSpec::loadContent));
    }

    public String dirName() {
        return dirName;
    }

    public URI uri() {
        return uri;
    }

    public String capitalisedName() {
        return Character.toUpperCase(name().charAt(0)) + name().substring(1).toLowerCase();
    }

    public static Optional<SchemaSpec> fromDir(final String dirName) {
        return Arrays.stream(values()).filter(spec -> spec.dirName.equals(dirName)).findAny();
    }

    public static Optional<String> contentFromUri(final URI uri) {
        return Arrays.stream(values())
                .map(spec -> spec.getContentFromUri(uri))
                .flatMap(Optional::stream)
                .findAny();
    }

    private Optional<String> getContentFromUri(final URI uri) {
        final URI normalized = normalize(uri);
        if (normalize(this.uri).equals(normalized)) {
            return Optional.of(content);
        }
        final String content = additional.get(normalized);
        return content == null ? Optional.empty() : Optional.of(content);
    }

    private static URI normalize(final URI uri) {
        final String uriString = uri.toString();
        if (uriString.endsWith("#")) {
            return URI.create(uriString.substring(0, uriString.length() - "#".length()));
        }
        return uri;
    }

    @SuppressFBWarnings(
            value = "URLCONNECTION_SSRF_FD",
            justification = "only called with hardcoded urls")
    private static String loadContent(final URI uri) {
        try {
            // Always load from https, as non-secure http redirect to https:
            final URL url =
                    uri.getScheme().equals("http")
                            ? new URL("https" + uri.toString().substring(4))
                            : uri.toURL();

            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(true);
            final int responseCode = connection.getResponseCode();

            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new UncheckedIOException(
                        new IOException(
                                "Failed to load content from " + uri + ", code: " + responseCode));
            }

            try (BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    connection.getInputStream(), StandardCharsets.UTF_8))) {
                final StringBuilder builder = new StringBuilder();

                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append(System.lineSeparator());
                }

                final String content = builder.toString();
                if (content.isBlank()) {
                    throw new UncheckedIOException(
                            new IOException("Blank content loaded from " + uri));
                }
                return content;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
