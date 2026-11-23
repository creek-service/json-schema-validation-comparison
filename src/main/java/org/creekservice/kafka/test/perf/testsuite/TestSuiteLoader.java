/*
 * Copyright 2023-2025 Creek Contributors (https://github.com/creek-service)
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
import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.databind.InjectableValues;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class TestSuiteLoader {

    private final Predicate<? super Path> userPredicate;

    /**
     * @param predicate predicate to control which test files to load.
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

        if (!Files.exists(rootDir.resolve("validation-test-schema.json"))) {
            throw new RuntimeException("rootDir does not contain test suites: " + rootDir);
        }

        final Path testsDir = rootDir.resolve("tests");
        final List<TestSuite> allSuites = loadAllSuites(testsDir);

        final Map<SchemaSpec, List<TestSuite>> grouped = groupBySpec(allSuites);

        final List<SpecTestSuites> specSuites =
                grouped.entrySet().stream()
                        .map(e -> new SpecTestSuites(e.getKey(), e.getValue()))
                        .sorted(Comparator.comparing(s -> s.spec().name()))
                        .collect(toList());

        return new JsonSchemaTestSuite(specSuites);
    }

    private List<TestSuite> loadAllSuites(final Path testsDir) {
        final List<TestSuite> allSuites = new ArrayList<>();

        try (Stream<Path> files = Files.walk(testsDir)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .filter(userPredicate)
                    .sorted()
                    .map(TestSuiteLoader::loadSuiteFile)
                    .map(TestSuiteFile::tests)
                    .forEach(allSuites::addAll);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        return allSuites;
    }

    private static Map<SchemaSpec, List<TestSuite>> groupBySpec(final List<TestSuite> suites) {
        final Map<SchemaSpec, List<TestSuite>> grouped =
                new TreeMap<>(Comparator.comparing(SchemaSpec::name));

        for (final TestSuite suite : suites) {
            for (final SchemaSpec spec : Arrays.asList(SchemaSpec.values())) {
                if (suite.compatibility().check(spec)) {
                    grouped.computeIfAbsent(spec, k -> new ArrayList<>()).add(suite);
                }
            }
        }

        return grouped;
    }

    private static TestSuiteFile loadSuiteFile(final Path suiteFile) {
        try {
            return TestSuiteMapper.MAPPER
                    .readerFor(TestSuiteFile.class)
                    .with(new InjectableValues.Std().addValue("suiteFilePath", suiteFile))
                    .readValue(suiteFile.toFile());
        } catch (final Exception e) {
            throw new RuntimeException("Failed to parse test suite: " + suiteFile, e);
        }
    }
}
