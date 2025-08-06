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

package org.creekservice.kafka.test.perf;

import static org.creekservice.kafka.test.perf.ProjectPaths.INCLUDES_ROOT;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.creekservice.kafka.test.perf.performance.util.JsonToMarkdownConvertor;
import org.creekservice.kafka.test.perf.performance.util.PerformanceDataValidator;
import org.openjdk.jmh.runner.options.CommandLineOptionException;
import org.openjdk.jmh.runner.options.CommandLineOptions;

/** Entry point for running the performance benchmarks. */
public final class PerformanceMain {

    private PerformanceMain() {}

    public static void main(final String[] suppliedArgs) throws Exception {
        final String benchmark = extractBenchmark(suppliedArgs);
        final Path jsonResultFile = INCLUDES_ROOT.resolve(benchmark + ".json");

        ensureOutputDirectory();

        runBenchmarks(suppliedArgs, jsonResultFile);

        validateJsonOutput(jsonResultFile);
        writeMarkdownOutput(jsonResultFile);
    }

    private static String extractBenchmark(final String[] args) {
        try {
            final CommandLineOptions cmdOptions = new CommandLineOptions(args);
            final List<String> benchmarks = cmdOptions.getIncludes();
            if (benchmarks.size() != 1) {
                throw new CommandLineOptionException(
                        "A single benchmark to run must be supplied. Got: " + benchmarks);
            }

            return benchmarks.get(0);
        } catch (CommandLineOptionException e) {
            System.err.println("Error parsing command line:");
            System.err.println(" " + e.getMessage());
            System.exit(1);
            return null;
        }
    }

    private static void ensureOutputDirectory() throws IOException {
        Files.createDirectories(INCLUDES_ROOT);
    }

    private static void runBenchmarks(final String[] suppliedArgs, final Path jsonResultFile)
            throws IOException {
        final String[] additionalArgs = {
            // Output results in csv format
            "-rf",
            "json",
            // To a named file
            "-rff",
            jsonResultFile.toString(),
            // Fail on Error
            "-foe",
            "true"
        };

        final String[] allArgs = new String[suppliedArgs.length + additionalArgs.length];
        System.arraycopy(additionalArgs, 0, allArgs, 0, additionalArgs.length);
        System.arraycopy(suppliedArgs, 0, allArgs, additionalArgs.length, suppliedArgs.length);

        org.openjdk.jmh.Main.main(allArgs);
    }

    private static void validateJsonOutput(final Path jsonResultFile) {
        new PerformanceDataValidator().validate(jsonResultFile);
    }

    private static void writeMarkdownOutput(final Path jsonResultFile) {
        new JsonToMarkdownConvertor().convert(jsonResultFile, INCLUDES_ROOT);
    }
}
