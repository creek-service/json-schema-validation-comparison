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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.InjectableValues;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class TestSuiteLoader {

    private static final Path OPTIONAL = Paths.get("optional");
    private static final Path FORMAT = Paths.get("format");

    private final Predicate<? super Path> userPredicate;

    /**
     * @param predicate predicate to control which test files to load.
     * @param funcTest {@code true} if running functional vs perf test.
     */
    public TestSuiteLoader(final Predicate<? super Path> predicate) {
        this.userPredicate = requireNonNull(predicate);
    }

    public JsonSchemaTestSuite load(final Path rootDir) {
        if (!Files.exists(rootDir)) {
            throw new RuntimeException(
                    "rootDir does not exist: "
                            + rootDir
                            + System.lineSeparator()
                            + "Do you need to run the clone-json-schema-test-suite Gradle task to"
                            + " clone the test repo?");
        }

        if (!Files.exists(rootDir.resolve("test-schema.json"))) {
            throw new RuntimeException("rootDir does not contain test suites: " + rootDir);
        }

        final Path remotesDir = rootDir.resolve("remotes");
        final Map<URI, String> remotes = loadRemotes(remotesDir);

        try (Stream<Path> specs = Files.list(rootDir.resolve("tests"))) {
            final List<SpecTestSuites> suites =
                    specs.filter(
                                    testDir ->
                                            SchemaSpec.fromDir(testDir.getFileName().toString())
                                                    .isPresent())
                            .map(
                                    testDir ->
                                            new SpecTestSuites(
                                                    SchemaSpec.fromDir(
                                                                    testDir.getFileName()
                                                                            .toString())
                                                            .orElseThrow(),
                                                    loadSuiteFromSpecDir(testDir)))
                            .sorted(Comparator.comparing(s -> s.spec().name()))
                            .collect(toList());

            return new JsonSchemaTestSuite(suites, remotes, remotesDir);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<URI, String> loadRemotes(final Path remotes) {

        final Function<Path, URI> createKey =
                path -> URI.create("http://localhost:1234/" + remotes.relativize(path).toString().replace("\\", "/"));

        final Function<Path, String> readContent =
                path -> {
                    try {
                        return Files.readString(path, UTF_8);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read file: " + path, e);
                    }
                };

        try (Stream<Path> walk = Files.walk(remotes)) {
            return walk.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .collect(Collectors.toMap(createKey, readContent));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<TestSuite> loadSuiteFromSpecDir(final Path testDir) {
        final List<TestSuite> suites = new ArrayList<>();

        try (Stream<Path> s = Files.list(testDir)) {
            s.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .filter(userPredicate)
                    .map(TestSuiteLoader::loadSuites)
                    .forEach(suites::addAll);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        if (!testDir.endsWith(OPTIONAL) && Files.isDirectory(testDir.resolve(OPTIONAL))) {
            suites.addAll(loadSuiteFromSpecDir(testDir.resolve(OPTIONAL)));
        }

        if (!testDir.endsWith(FORMAT) && Files.isDirectory(testDir.resolve(FORMAT))) {
            suites.addAll(loadSuiteFromSpecDir(testDir.resolve(FORMAT)));
        }

        return suites;
    }

    private static List<TestSuite> loadSuites(final Path suiteFile) {
        try {
            return TestSuiteMapper.MAPPER
                    .readerFor(new TypeReference<List<TestSuite>>() {})
                    .with(new InjectableValues.Std().addValue("suiteFilePath", suiteFile))
                    .readValue(suiteFile.toFile());
        } catch (final Exception e) {
            throw new RuntimeException("Failed to parse test suite: " + suiteFile, e);
        }
    }
}
