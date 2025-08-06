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

package org.creekservice.kafka.test.perf.performance.util;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.creekservice.kafka.test.perf.implementations.Implementation;
import org.creekservice.kafka.test.perf.implementations.Implementations;
import org.creekservice.kafka.test.perf.performance.util.model.PerformanceResult;
import org.creekservice.kafka.test.perf.testsuite.SchemaSpec;

/**
 * Validate the output results will be compatible and match expected patterns used in
 * performance.md.
 */
public final class PerformanceDataValidator {

    private final PerformanceJsonReader reader;
    private final List<Implementation> implementations;
    private final Set<String> specs;

    public PerformanceDataValidator() {
        this(new PerformanceJsonReader());
    }

    PerformanceDataValidator(final PerformanceJsonReader reader) {
        this.reader = requireNonNull(reader, "reader");
        this.implementations = List.copyOf(Implementations.all());
        this.specs =
                Arrays.stream(SchemaSpec.values())
                        .map(SchemaSpec::capitalisedName)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public void validate(final Path jsonResults) {
        final PerformanceResult[] results = reader.read(jsonResults);

        for (final PerformanceResult result : results) {
            validate(result);
        }
    }

    // Benchmark function name should be in format: measure<schema-spec>_<implementation-name>
    // e.g. measureDraft_4_Medeia, or measureDraft_2020_12_Skema
    private static final Pattern METHOD_PATTERN =
            Pattern.compile(
                    "measure(?<draft>Draft[_0-9]+)_(?<impl>"
                            + Implementation.MetaData.SHORT_NAME_PATTERN.pattern()
                            + ")");

    private void validate(final PerformanceResult result) {
        final Matcher matcher = METHOD_PATTERN.matcher(result.testCase());
        if (!matcher.matches()) {
            throw new ValidationException(
                    result,
                    "with a name that does not match the expected pattern."
                            + " Expected pattern: "
                            + METHOD_PATTERN.pattern());
        }

        validateDraft(matcher.group("draft"), result);
        validateImplementationName(matcher.group("impl"), result);
    }

    private void validateDraft(final String draft, final PerformanceResult result) {
        if (!specs.contains(draft)) {
            throw new ValidationException(
                    result,
                    "with a name that does not contain a valid schema specification draft."
                            + System.lineSeparator()
                            + "Available versions: "
                            + specs
                            + System.lineSeparator()
                            + "Detected version: "
                            + draft);
        }
    }

    private void validateImplementationName(
            final String implShortName, final PerformanceResult result) {
        if (implementations.stream()
                .noneMatch(impl -> impl.metadata().shortName().equals(implShortName))) {
            throw new ValidationException(
                    result,
                    "with a name that not end with a known implementation's short name."
                            + System.lineSeparator()
                            + "Detected short name: "
                            + implShortName);
        }
    }

    private static final class ValidationException extends IllegalArgumentException {
        ValidationException(final PerformanceResult result, final String msg) {
            super(
                    "The JSON benchmark results contain a benchmark method "
                            + msg
                            + System.lineSeparator()
                            + "Method name: "
                            + result.testClass()
                            + "."
                            + result.testCase());
        }
    }
}
