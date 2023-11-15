[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![build](https://github.com/creek-service/json-schema-validation-comparison/actions/workflows/build.yml/badge.svg)](https://github.com/creek-service/json-schema-validation-comparison/actions/workflows/build.yml)

# JSON Schema Validation comparison

Feature and performance comparison of different JVM-based implementations of JSON schema validators.

The results of this comparison can be found on [here][micro-site].

## Note to maintainers

If you are the maintainer of one of the above implementations, and you feel your implementation is poorly represented,
or you maintain an JVM-based implementation not covered yet covered in this comparison, then please feel free to raise a PR.
See the [Contributing](#contributing) section below.

## Contributing

### Adding a new validator implementation

Adding a new validator implementation is relatively straight forward and very welcome:

1. First, take a look at the [micro-site][micro-site], as it gives some explanation of what is being tested. 
2. Clone the repo and pull it down locally, creating your own branch to work in.
3. Add necessary dependencies to [build.gradle.kts](build.gradle.kts).
4. Ensure GitHub's Dependabot will update the new dependency version.
   This will ensure the site updates when new versions are released.
   See the [Dependabot version updates docs](https://docs.github.com/en/code-security/dependabot/dependabot-version-updates/about-dependabot-version-updates).
5. Add a new implementation of [Implementation](src/main/java/org/creekservice/kafka/test/perf/implementations/Implementation.java) 
   to the [main implementations](src/main/java/org/creekservice/kafka/test/perf/implementations) package for the new validator library.
   See JavaDocs and other implementations for help.
6. Register your new Implementation type in [Implementations.java](src/main/java/org/creekservice/kafka/test/perf/implementations/Implementations.java).
   This will ensure the new implementation is included in the docs and included in the functional test
7. Run [ImplementationTest.java](src/test/java/org/creekservice/kafka/test/perf/implementations/ImplementationTest.java).
   This unit test will test each implementation, including yours.
   Ensure tests pass!
8. Manually add appropriate benchmark methods to [JsonSerdeBenchmark.java](src/main/java/org/creekservice/kafka/test/perf/performance/JsonSerdeBenchmark.java)
   and [JsonValidateBenchmark.java](src/main/java/org/creekservice/kafka/test/perf/performance/JsonValidateBenchmark.java).
   This is currently manual as JMH library does provide a way to generate these automatically.
   There should be one test per supported draft version. See JavaDocs and the other methods in these classes for examples.
9. Run `./gradlew` to format your code, perform static analysis and run the tests. 
   Ensure this passes!
10. Follow [these instructions](docs) to build and view the website, and ensure your new implementation data is included in tables and charts.
11. Raise a PR with your changes.

### Running things locally

#### Feature comparison

Run the feature comparison locally with `./gradlew runFunctionalTests`,
or the [latest results](https://www.creekservice.org/json-schema-validation-comparison/functional),
or previous runs are available in the [GitHub pages workflow runs on GitHub][GitHubPagesWfRuns].

Running the functional tests will create result files in the `docs/_include` directory, ready for Jekyll to inject into the [micro-site][micro-site].

Generated files:

| filename                  | description                                                                                                                                                                           | use                                                                     |
|---------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------|
| `functional-summary.json` | JSON document containing a summary of pass/fail rates of required/optional test cases for each implementation, per supported JSON schema version.                                     | Used to build functional tables and charts in [micro-site][micro-site]. |
| `functional-summary.md`   | Markdown document containing a summary of pass/fail rates of required/optional test cases for each implementation, per supported JSON schema version.                                 | Appended to the GitHub workflow job                                     |
| `per-draft.md`            | Markdown document containing one table for each implementation and supported schema specification combination, showing the number of test cases that pass and fail in each test file. | Appended to the GitHub workflow job                                     |

#### Performance comparison

Run the performance comparison locally with `./gradlew runBenchmarks`,
or the [latest results](https://www.creekservice.org/json-schema-validation-comparison/performance),
or previous runs are available in the [GitHub pages workflow runs on GitHub][GitHubPagesWfRuns].

See benchmark classes in the [performance package](src/main/java/org/creekservice/kafka/test/perf/performance) .

Running the performance benchmarks will create result files in the `docs/_include` directory, ready for Jekyll to inject into the [micro-site][micro-site].

Running the performance benchmarks takes a long time. Running `./gradlew runBenchmarkSmokeTest` will run the same benchmarks in a matter of minutes, which can be useful for testing and generating data for the [micro-site][micro-site].

Generated files:

| filename                   | description                                                            | use                                                                     |
|----------------------------|------------------------------------------------------------------------|-------------------------------------------------------------------------|
| `benchmark_results.json`   | A JSON document containing the results of the performance benchmarking | Used to build functional tables and charts in [micro-site][micro-site]. |
| `JsonSerdeBenchmark.md`    | Markdown document containing the results of this benchmark class.      | Appended to the GitHub workflow job                                     |
| `JsonValidateBenchmark.md` | Markdown document containing the results of this benchmark class.      | Appended to the GitHub workflow job                                     |

[GitHubPagesWfRuns]: https://github.com/creek-service/json-schema-validation-comparison/actions/workflows/gh-pages.yml
[micro-site]: https://www.creekservice.org/json-schema-validation-comparison/