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

package org.creekservice.kafka.test.perf;

import static org.creekservice.kafka.test.perf.ProjectPaths.INCLUDES_ROOT;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.creekservice.kafka.test.perf.performance.util.JsonToMarkdownConvertor;
import org.creekservice.kafka.test.perf.performance.util.PerformanceDataValidator;

/** Entry point for running the performance benchmarks. */
public final class PerformanceMain {

    private static final Path JSON_RESULTS = INCLUDES_ROOT.resolve("benchmark_results.json");

    private PerformanceMain() {}

    public static void main(final String[] suppliedArgs) throws Exception {
        runBenchmarks(suppliedArgs);
        validateJsonOutput();
        writeMarkdownOutput();
    }

    private static void runBenchmarks(final String[] suppliedArgs) throws IOException {
        final String[] additionalArgs = {
            // Output results in csv format
            "-rf",
            "json",
            // To a named file
            "-rff",
            JSON_RESULTS.toString()
        };

        final String[] allArgs = new String[suppliedArgs.length + additionalArgs.length];
        System.arraycopy(suppliedArgs, 0, allArgs, 0, suppliedArgs.length);
        System.arraycopy(additionalArgs, 0, allArgs, suppliedArgs.length, additionalArgs.length);

        Files.createDirectories(INCLUDES_ROOT);

        org.openjdk.jmh.Main.main(allArgs);
    }

    private static void validateJsonOutput() {
        new PerformanceDataValidator().validate(JSON_RESULTS);
    }

    private static void writeMarkdownOutput() {
        new JsonToMarkdownConvertor().convert(JSON_RESULTS, INCLUDES_ROOT);
    }
}
