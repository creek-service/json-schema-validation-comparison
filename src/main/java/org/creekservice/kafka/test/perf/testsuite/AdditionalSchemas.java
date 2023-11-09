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

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Map;

/**
 * Interface for impls to use to load meta-schemas and JSON-Schema-Test-Suite 'remote' schemas,
 * <i>without</i> IO operations, (Which would mess with performance results).
 */
public final class AdditionalSchemas {

    private final Map<URI, String> remotes;
    private final Path remotesDir;

    public AdditionalSchemas(final Map<URI, String> remotes, final Path remotesDir) {
        this.remotes = Map.copyOf(remotes);
        this.remotesDir = requireNonNull(remotesDir, "remotesDir");
    }

    /**
     * Load a remote schema.
     *
     * @param uri the schema id to load.
     * @return the schema content
     * @throws RuntimeException on unknown schema.
     */
    public String load(final String uri) {
        return load(URI.create(uri));
    }

    /**
     * Load a remote schema.
     *
     * @param uri the schema id to load.
     * @return the schema content
     * @throws RuntimeException on unknown schema.
     */
    public String load(final URI uri) {
        if (!uri.getScheme().startsWith("http")) {
            throw new UnsupportedOperationException("Unsupported schema in: " + uri);
        }
        final URI normalised = normalize(uri);

        final String remote = remotes.get(normalised);
        if (remote != null) {
            return remote;
        }

        return SchemaSpec.contentFromUri(uri)
                .orElseThrow(
                        () ->
                                new UnsupportedOperationException(
                                        "Loading of remote content disabled: " + uri));
    }

    /**
     * @return content of JSON-Schema-Test-Suite 'remote' schemas
     */
    public Map<URI, String> remotes() {
        return Map.copyOf(remotes);
    }

    /**
     * @return location where remotes are being loaded from.
     */
    public Path remotesDir() {
        return remotesDir;
    }

    private static URI normalize(final URI uri) {
        try {
            return new URI(
                    uri.getScheme(), uri.getAuthority(), uri.getPath(), uri.getRawQuery(), null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
