[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![build](https://github.com/creek-service/json-schema-validation-comparison/actions/workflows/build.yml/badge.svg)](https://github.com/creek-service/json-schema-validation-comparison/actions/workflows/build.yml)

# JSON Schema Validation comparison

Feature and performance comparison of different JVM-based implementations of JSON schema validators.

The results of this comparison can be found on [here](https://www.creekservice.org/json-schema-validation-comparison/).

## Note to maintainers

If you are the maintainer of one of the above implementations, and you feel your implementation is poorly represented,
or you maintain an JVM-based implementation not covered yet covered in this comparison, then please feel free to raise a PR.
See the [Contributing](#contributing) section below.

## Feature comparison

Run the feature comparison locally with `./gradlew runFunctionalTests`, 
or the [latest results](https://www.creekservice.org/json-schema-validation-comparison/functional), 
or view previous runs on [GitHub][functionalTestRuns].

Runs each implementation through the standard [JSON Schema Test Suite][JSON-Schema-Test-Suite].
The suite contains both positive and negative test cases, i.e. JSON that should both pass and fail validation,
and covers all schema specifications, i.e. draft-03 through to the latest.

Running the functional tests will create result files in the `build/reports/creek` directory.

### functional-summary.md

This report contains a summary of pass/fail rates of required/optional test cases for each implementation,
per supported JSON schema version.

For example:

| Impl         | Overall                                                                                | DRAFT_03                                                                         | DRAFT_04                                                                           | DRAFT_06                                                                           | DRAFT_07                                                                           | DRAFT_2019_09                                                                       | DRAFT_2020_12                                                                        |
|--------------|----------------------------------------------------------------------------------------|----------------------------------------------------------------------------------|------------------------------------------------------------------------------------|------------------------------------------------------------------------------------|------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------|
| SchemaFriend | score: 98.0<br>pass: r:5051 (98.8%) o:2332 (95.7%)<br>fail: r:60 (1.2%) o:106 (4.3%)   | score: 98.4<br>pass: r:435 (100.0%) o:104 (93.7%)<br>fail: r:0 (0.0%) o:7 (6.3%) | score: 98.5<br>pass: r:590 (99.8%) o:234 (94.4%)<br>fail: r:1 (0.2%) o:14 (5.6%)   | score: 98.6<br>pass: r:791 (99.6%) o:294 (95.5%)<br>fail: r:3 (0.4%) o:14 (4.5%)   | score: 98.8<br>pass: r:875 (99.7%) o:510 (96.0%)<br>fail: r:3 (0.3%) o:21 (4.0%)   | score: 98.0<br>pass: r:1178 (98.6%) o:591 (96.1%)<br>fail: r:17 (1.4%) o:24 (3.9%)  | score: 96.7<br>pass: r:1182 (97.0%) o:599 (95.8%)<br>fail: r:36 (3.0%) o:26 (4.2%)   |
| Snow         | score: 97.6<br>pass: r:2823 (98.5%) o:1381 (95.0%)<br>fail: r:44 (1.5%) o:73 (5.0%)    |                                                                                  |                                                                                    | score: 98.0<br>pass: r:783 (98.6%) o:296 (96.1%)<br>fail: r:11 (1.4%) o:12 (3.9%)  | score: 98.1<br>pass: r:869 (99.0%) o:508 (95.7%)<br>fail: r:9 (1.0%) o:23 (4.3%)   | score: 96.9<br>pass: r:1171 (98.0%) o:577 (93.8%)<br>fail: r:24 (2.0%) o:38 (6.2%)  |                                                                                      |
| Medeia       | score: 96.3<br>pass: r:2250 (99.4%) o:946 (87.0%)<br>fail: r:13 (0.6%) o:141 (13.0%)   |                                                                                  | score: 95.7<br>pass: r:587 (99.3%) o:210 (84.7%)<br>fail: r:4 (0.7%) o:38 (15.3%)  | score: 96.4<br>pass: r:789 (99.4%) o:270 (87.7%)<br>fail: r:5 (0.6%) o:38 (12.3%)  | score: 96.6<br>pass: r:874 (99.5%) o:466 (87.8%)<br>fail: r:4 (0.5%) o:65 (12.2%)  |                                                                                     |                                                                                      |
| Justify      | score: 95.4<br>pass: r:2146 (94.8%) o:1055 (97.1%)<br>fail: r:117 (5.2%) o:32 (2.9%)   |                                                                                  | score: 95.4<br>pass: r:560 (94.8%) o:241 (97.2%)<br>fail: r:31 (5.2%) o:7 (2.8%)   | score: 95.7<br>pass: r:755 (95.1%) o:301 (97.7%)<br>fail: r:39 (4.9%) o:7 (2.3%)   | score: 95.1<br>pass: r:831 (94.6%) o:513 (96.6%)<br>fail: r:47 (5.4%) o:18 (3.4%)  |                                                                                     |                                                                                      |
| Everit       | score: 95.0<br>pass: r:2204 (97.4%) o:953 (87.7%)<br>fail: r:59 (2.6%) o:134 (12.3%)   |                                                                                  | score: 95.8<br>pass: r:581 (98.3%) o:219 (88.3%)<br>fail: r:10 (1.7%) o:29 (11.7%) | score: 95.5<br>pass: r:770 (97.0%) o:280 (90.9%)<br>fail: r:24 (3.0%) o:28 (9.1%)  | score: 94.2<br>pass: r:853 (97.2%) o:454 (85.5%)<br>fail: r:25 (2.8%) o:77 (14.5%) |                                                                                     |                                                                                      |
| Vert.x       | score: 93.7<br>pass: r:3756 (96.8%) o:1710 (84.7%)<br>fail: r:126 (3.2%) o:309 (15.3%) |                                                                                  | score: 96.2<br>pass: r:580 (98.1%) o:224 (90.3%)<br>fail: r:11 (1.9%) o:24 (9.7%)  |                                                                                    | score: 94.0<br>pass: r:860 (97.9%) o:436 (82.1%)<br>fail: r:18 (2.1%) o:95 (17.9%) | score: 94.1<br>pass: r:1162 (97.2%) o:522 (84.9%)<br>fail: r:33 (2.8%) o:93 (15.1%) | score: 92.2<br>pass: r:1154 (94.7%) o:528 (84.5%)<br>fail: r:64 (5.3%) o:97 (15.5%)  |
| sKema        | score: 93.5<br>pass: r:1192 (97.9%) o:503 (80.5%)<br>fail: r:26 (2.1%) o:122 (19.5%)   |                                                                                  |                                                                                    |                                                                                    |                                                                                    |                                                                                     | score: 93.5<br>pass: r:1192 (97.9%) o:503 (80.5%)<br>fail: r:26 (2.1%) o:122 (19.5%) |
| NetworkNt    | score: 93.1<br>pass: r:4451 (95.2%) o:2023 (86.9%)<br>fail: r:225 (4.8%) o:304 (13.1%) |                                                                                  | score: 96.8<br>pass: r:581 (98.3%) o:229 (92.3%)<br>fail: r:10 (1.7%) o:19 (7.7%)  | score: 95.2<br>pass: r:773 (97.4%) o:273 (88.6%)<br>fail: r:21 (2.6%) o:35 (11.4%) | score: 93.9<br>pass: r:853 (97.2%) o:447 (84.2%)<br>fail: r:25 (2.8%) o:84 (15.8%) | score: 92.1<br>pass: r:1122 (93.9%) o:533 (86.7%)<br>fail: r:73 (6.1%) o:82 (13.3%) | score: 90.7<br>pass: r:1122 (92.1%) o:541 (86.6%)<br>fail: r:96 (7.9%) o:84 (13.4%)  |

Each populated cell details the **r**equired and **o**ptional passed and failed test case counts and percentages by Schema specification version, and overall.
Underneath there is a 'score' for each implementation, out of 100.
The score weights test results of _required_ features at triple _optional_ features, meaning 75% of the score is reserved for _required_ features,
whereas _optional_ features only account for a maximum 25% of the score.

### functional-summary.json

As above, but stored in JSON notation.

This is used to drive the [results micro-site](https://www.creekservice.org/json-schema-validation-comparison/).

### per-draft.md

This report contains one table for each implementation and supported schema specification combination, 
showing the number of test cases that pass and fail in each test file.

For example, 

Medeia: DRAFT_07:

| suite                                      | pass | fail | total |
|--------------------------------------------|------|------|-------|
| additionalItems.json                       | 18   | 0    | 18    |
| additionalProperties.json                  | 16   | 0    | 16    |
| allOf.json                                 | 30   | 0    | 30    |
| anyOf.json                                 | 18   | 0    | 18    |
| boolean_schema.json                        | 18   | 0    | 18    |
| const.json                                 | 50   | 0    | 50    |
| contains.json                              | 21   | 0    | 21    |
| default.json                               | 7    | 0    | 7     |
| definitions.json                           | 2    | 0    | 2     |
| dependencies.json                          | 33   | 3    | 36    |
| enum.json                                  | 33   | 0    | 33    |
| exclusiveMaximum.json                      | 4    | 0    | 4     |
| exclusiveMinimum.json                      | 4    | 0    | 4     |
| format.json                                | 102  | 0    | 102   |
| id.json                                    | 7    | 0    | 7     |
| if-then-else.json                          | 26   | 0    | 26    |
| infinite-loop-detection.json               | 2    | 0    | 2     |
| items.json                                 | 28   | 0    | 28    |
| maxItems.json                              | 6    | 0    | 6     |
| maxLength.json                             | 7    | 0    | 7     |
| maxProperties.json                         | 10   | 0    | 10    |
| maximum.json                               | 8    | 0    | 8     |
| minItems.json                              | 6    | 0    | 6     |
| minLength.json                             | 7    | 0    | 7     |
| minProperties.json                         | 8    | 0    | 8     |
| minimum.json                               | 11   | 0    | 11    |
| multipleOf.json                            | 10   | 0    | 10    |
| not.json                                   | 12   | 0    | 12    |
| oneOf.json                                 | 27   | 0    | 27    |
| optional/bignum.json                       | 9    | 0    | 9     |
| optional/content.json                      | 10   | 0    | 10    |
| optional/cross-draft.json                  | 1    | 1    | 2     |
| optional/ecmascript-regex.json             | 55   | 19   | 74    |
| optional/float-overflow.json               | 1    | 0    | 1     |
| optional/format/date-time.json             | 23   | 2    | 25    |
| optional/format/date.json                  | 47   | 0    | 47    |
| optional/format/email.json                 | 11   | 4    | 15    |
| optional/format/hostname.json              | 18   | 0    | 18    |
| optional/format/idn-email.json             | 8    | 2    | 10    |
| optional/format/idn-hostname.json          | 38   | 13   | 51    |
| optional/format/ipv4.json                  | 14   | 1    | 15    |
| optional/format/ipv6.json                  | 29   | 11   | 40    |
| optional/format/iri-reference.json         | 13   | 0    | 13    |
| optional/format/iri.json                   | 14   | 1    | 15    |
| optional/format/json-pointer.json          | 38   | 0    | 38    |
| optional/format/regex.json                 | 8    | 0    | 8     |
| optional/format/relative-json-pointer.json | 15   | 3    | 18    |
| optional/format/time.json                  | 39   | 6    | 45    |
| optional/format/unknown.json               | 7    | 0    | 7     |
| optional/format/uri-reference.json         | 13   | 0    | 13    |
| optional/format/uri-template.json          | 9    | 1    | 10    |
| optional/format/uri.json                   | 26   | 0    | 26    |
| optional/non-bmp-regex.json                | 12   | 0    | 12    |
| pattern.json                               | 9    | 0    | 9     |
| patternProperties.json                     | 23   | 0    | 23    |
| properties.json                            | 27   | 1    | 28    |
| propertyNames.json                         | 13   | 0    | 13    |
| ref.json                                   | 76   | 0    | 76    |
| refRemote.json                             | 21   | 0    | 21    |
| required.json                              | 16   | 0    | 16    |
| type.json                                  | 80   | 0    | 80    |
| uniqueItems.json                           | 69   | 0    | 69    |
| unknownKeyword.json                        | 3    | 0    | 3     |


### Feature comparison conclusions

At the time of writing, `ScheamFriend` comes out as the clear winner of the functional test, with support for all Schema specification, at the time of writing, _and_ the highest overall score.

Ignoring which implementations support which drafts for a moment, a rough ranking on functionality would be:

![Feature-comparison-score.svg](img/Feature-comparison-score.svg)

Obviously, your own requirements around which specification drafts your want, or need, to use may exclude some of these.

There are also a couple of notes to call out for different implementations around features outside of those covered by the standard tests.

| Implementation                       | Notes                                                                                                                                                                         |
|--------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [Vert.x Json Schema][1]              | Brings in Netty as a dependency, which seems unnecessary.<br>There doesn't seem to be a way to disable loading schemas from remote locations or injecting referenced schemas. |
| [jsonschemafriend][2]                |                                                                                                                                                                               |
| [networknt/json-schema-validator][3] |                                                                                                                                                                               |
| [Snow][4]                            | This is intended as a reference implementation.                                                                                                                               |
| [everit-org/json-schema][5]          | Deprecated. Replaced by [erosb/json-sKema][8].                                                                                                                                |
| [Justify][6]                         | No sign of active development :(  - Last released Nov, 2020.                                                                                                                  |
| [worldturner/medeia-validator][7]    | No sign of active development :(  - Last released Jun, 2019.                                                                                                                  |
| [erosb/json-sKema][8]                | Replaces [everit-org/json-schema][5]. Looks to still be in initial development...                                                                                             |

## Performance comparison

Run the performance comparison locally with `./gradlew runBenchmarks`, or view previous runs on [GitHub][performanceBenchmarkRuns].

How fast is the implementation at validating JSON? To find out, two different performance suites were run using
the [Java Microbenchmark Harness][jhm]:

1. Performance test running the standard [JSON Schema Test Suite][JSON-Schema-Test-Suite].
2. Performance test serializing and deserializing Java Pojos to JSON and back.

The first of these benchmark covers a wide range of JSON schema functionality, while the second focuses on a more
real-world example, using a small common subset of functionality, in the context of using schema validated JSON 
as a serialization format.  Combined, these should give a good comparison of performance.

### JSON schema test suite benchmark

The `JsonValidateBenchmark` benchmark measures the average time taken to run through all _positive_ test cases in the standard 
[JSON Schema Test Suite][JSON-Schema-Test-Suite], by schema specification.

The benchmark excludes negative test cases and the cost of parsing the schema and building the validator logic,
leaving the benchmark is focused on measuring cost of validation.

The benchmark excludes _negative_ test cases, i.e. test cases with data that should _not_ pass validation, for two
reasons:

1. In most use-cases, and specifically the Kafka SerDe use-case we're investigating for, validation fails should be very rare.
2. The cost of error handling varied between different implementations, generally correlated to the richness of the error messages.
   Including negative cases would penalise implementations for useful error messages.

The benchmark excludes the cost of parsing the schema and building the necessary validator logic as in most use-cases, and 
specifically the Kafka SerDe use-case we're investigating for, schemas don't tend to evolve or change often, meaning the
cost of validation is much more important than the cost of building the validation logic.

Example output:

```
Benchmark                                                Mode  Cnt     Score    Error  Units
JsonValidateBenchmark.measureDraft_2019_09_NetworkNt     avgt   20     6.017 ±  0.216  ms/op
JsonValidateBenchmark.measureDraft_2019_09_SchemaFriend  avgt   20     1.482 ±  0.005  ms/op
JsonValidateBenchmark.measureDraft_2019_09_Snow          avgt   20   316.178 ± 28.242  ms/op
JsonValidateBenchmark.measureDraft_2019_09_Vertx         avgt   20     3.818 ±  0.028  ms/op
JsonValidateBenchmark.measureDraft_2020_12_NetworkNt     avgt   20     7.305 ±  0.073  ms/op
JsonValidateBenchmark.measureDraft_2020_12_SchemaFriend  avgt   20     1.654 ±  0.005  ms/op
JsonValidateBenchmark.measureDraft_2020_12_Skema         avgt   20     2.812 ±  0.015  ms/op
JsonValidateBenchmark.measureDraft_2020_12_Vertx         avgt   20     3.669 ±  0.019  ms/op
JsonValidateBenchmark.measureDraft_3_SchemaFriend        avgt   20     0.235 ±  0.005  ms/op
JsonValidateBenchmark.measureDraft_4_Everit              avgt   20     0.328 ±  0.006  ms/op
JsonValidateBenchmark.measureDraft_4_Justify             avgt   20     0.634 ±  0.009  ms/op
JsonValidateBenchmark.measureDraft_4_Medeia              avgt   20     0.346 ±  0.006  ms/op
JsonValidateBenchmark.measureDraft_4_NetworkNt           avgt   20     1.086 ±  0.004  ms/op
JsonValidateBenchmark.measureDraft_4_SchemaFriend        avgt   20     0.480 ±  0.017  ms/op
JsonValidateBenchmark.measureDraft_4_Vertx               avgt   20     1.362 ±  0.006  ms/op
JsonValidateBenchmark.measureDraft_6_Everit              avgt   20     0.400 ±  0.003  ms/op
JsonValidateBenchmark.measureDraft_6_Justify             avgt   20     0.816 ±  0.008  ms/op
JsonValidateBenchmark.measureDraft_6_Medeia              avgt   20     0.416 ±  0.007  ms/op
JsonValidateBenchmark.measureDraft_6_NetworkNt           avgt   20     1.771 ±  0.044  ms/op
JsonValidateBenchmark.measureDraft_6_SchemaFriend        avgt   20     0.700 ±  0.018  ms/op
JsonValidateBenchmark.measureDraft_6_Snow                avgt   20    78.241 ±  6.515  ms/op
JsonValidateBenchmark.measureDraft_7_Everit              avgt   20     0.508 ±  0.005  ms/op
JsonValidateBenchmark.measureDraft_7_Justify             avgt   20     1.044 ±  0.019  ms/op
JsonValidateBenchmark.measureDraft_7_Medeia              avgt   20     0.666 ±  0.007  ms/op
JsonValidateBenchmark.measureDraft_7_NetworkNt           avgt   20     2.573 ±  0.032  ms/op
JsonValidateBenchmark.measureDraft_7_SchemaFriend        avgt   20     0.918 ±  0.012  ms/op
JsonValidateBenchmark.measureDraft_7_Snow                avgt   20    76.627 ±  6.336  ms/op
JsonValidateBenchmark.measureDraft_7_Vertx               avgt   20     2.141 ±  0.072  ms/op
```
Note: results from running on 2021 Macbook Pro, M1 Max: 2.06 - 3.22 GHz, in High Power mode, JDK 17.0.6

Each of the following graphs compares the average time it took each implementation to validate all of its **positive**
test cases.

The following caveats apply to the results:
1. The `Snow` implementation has been removed from the graphs, as its so slow that it makes the graph unreadable when trying to compare the other implementations.
2. Comparison of time between the different drafts, i.e. between the different charts, is fairly meaningless, as the number of tests changes. Latter drafts generally have move test cases, meaning they take longer to run.
3. When comparing times a graph, remember that the time only covers each implementation's positive test cases. This means implementations with less functional coverage have less positive cases to handle.   

![JsonValidateBenchmark-Draft-4.svg](img/JsonValidateBenchmark-Draft-4.svg)

![JsonValidateBenchmark-Draft-6.svg](img/JsonValidateBenchmark-Draft-6.svg)

![JsonValidateBenchmark-Draft-7.svg](img/JsonValidateBenchmark-Draft-7.svg)

![JsonValidateBenchmark-Draft-2019-0.svg](img/JsonValidateBenchmark-Draft-2019-0.svg)

![JsonValidateBenchmark-Draft-2020-12.svg](img/JsonValidateBenchmark-Draft-2020-12.svg)

### Schema validated JSON (de)serialization benchmark

The `JsonSerdeBenchmark` benchmark measures the average time taken to serialize a simple Java object, including polymorphism, to JSON and back,
validating the intermediate JSON data on both legs of the journey.

This is a more real-world test, keeping to the basics of what's possible with JSON schemas, as that's what most use-cases need. 
Therefore, this benchmark includes the cost of serialization, deserialization and two validations of the JSON document.
JSON (de)serialization is generally handled by Jackson, except where this isn't compatible with the validation implementation.
The cost of just Jackson (de)serialization is included, i.e. no validation, in the results below for comparison.

Example results:

```
Benchmark                                                Mode  Cnt     Score    Error  Units
JsonSerdeBenchmark.measureConfluentRoundTrip             avgt   20   107.620 ±  0.546  us/op
JsonSerdeBenchmark.measureEveritRoundTrip                avgt   20    99.747 ±  1.894  us/op
JsonSerdeBenchmark.measureJacksonIntermediateRoundTrip   avgt   20     4.032 ±  0.162  us/op
JsonSerdeBenchmark.measureJacksonRoundTrip               avgt   20     4.114 ±  0.204  us/op
JsonSerdeBenchmark.measureJustifyRoundTrip               avgt   20    72.263 ±  0.811  us/op
JsonSerdeBenchmark.measureMedeiaRoundTrip                avgt   20    30.055 ±  0.351  us/op
JsonSerdeBenchmark.measureNetworkNtRoundTrip             avgt   20  1195.955 ± 33.623  us/op
JsonSerdeBenchmark.measureSchemaFriendRoundTrip          avgt   20   142.186 ±  4.105  us/op
JsonSerdeBenchmark.measureSkemaRoundTrip                 avgt   20   166.841 ±  0.303  us/op
JsonSerdeBenchmark.measureSnowRoundTrip                  avgt   20   603.705 ±  4.627  us/op
JsonSerdeBenchmark.measureVertxRoundTrip                 avgt   20   514.517 ±  1.337  us/op
```
Note: results from running on 2021 Macbook Pro, M1 Max: 2.06 - 3.22 GHz, in High Power mode, JDK 17.0.6

![JsonSerdeBenchmark Results.svg](img/JsonSerdeBenchmark-Results.svg)

### Performance comparison conclusions

At the time of writing, `Medeia` comes as a clear winner for speed, with `Justify` and then `Everit` not far behind.
Unfortunately, `Medeia` and `Justify` look to no longer be maintained, and `Everit` is deprecated in favour of a new implementation that seems both incomplete and slower.
Plus, neither of them handle the latest drafts of the JSON schema standard.
If these three are excluded from the results, then the clear winner is `SchemaFriend`.

## Conclusions

Hopefully this comparison is useful. The intended use-case will likely dictate which implementation(s) are suitable.

If your use-case requires ultimate speed, doesn't require advanced features or support for the later draft specifications, 
and you're happy with the maintenance risk associated with them, then either `Medeia` or `Everit` may be the implementation for you.
It's worth pointing out that [Confluent][confluent]'s own JSON serde internally use `Everit`, which may mean they'll be helping to support it going forward.

Alternatively, if you're either uneasy using deprecated or unmaintained libraries, or need more functionality or support for the latest drafts, 
then these results would suggest you take a look at `SchemaFriend`: it comes out on top for functionality and is only beaten on performance by the unmaintained or deprecated `Medeia` and `Everit`. 

Note: The author of this repository is not affiliated with any of the implementations covered by this test suite.

## Contributing

### Adding a new validator implementation

Adding a new validator implementation is relatively straight forward and very welcome:

1. Clone the repo and pull it down locally, creating your own branch to work in.
2. Add necessary dependencies to [build.gradle.kts](build.gradle.kts).
3. Add a new implementation of [Implementation](src/main/java/org/creekservice/kafka/test/perf/implementations/Implementation.java) 
   to the [main implementations](src/main/java/org/creekservice/kafka/test/perf/implementations) package for the new validator library.
   See JavaDocs and other implementations for help.
4. Add a unit test class for your new implementation to the [test implementations](src/test/java/org/creekservice/kafka/test/perf/implementations) package.
   This should subtype [ImplementationTest.java](src/test/java/org/creekservice/kafka/test/perf/implementations/ImplementationTest.java).
   The unit test class needs to content. See other implementations for examples.
   Ensure tests pass!
5. Register your new Implementation type in [Implementations.java](src/main/java/org/creekservice/kafka/test/perf/implementations/Implementations.java).
   This will ensure the new implementation is included in the docs and included in the functional test
6. Manually add appropriate benchmark methods to [JsonSerdeBenchmark.java](src/main/java/org/creekservice/kafka/test/perf/performance/JsonSerdeBenchmark.java)
   and [JsonValidateBenchmark.java](src/main/java/org/creekservice/kafka/test/perf/performance/JsonValidateBenchmark.java).
   This is currently manual as JMH library does provide a way to generate these automatically.
   There should be one test per supported draft version. See the other methods in these classes for examples.
7. Run `./gradlew` to format your code, perform static analysis and run the tests. 
   Ensure this passes!
8. Raise a PR with your changes.


[1]: https://github.com/eclipse-vertx/vertx-json-schema
[2]: https://github.com/jimblackler/jsonschemafriend
[3]: https://github.com/networknt/json-schema-validator
[4]: https://github.com/ssilverman/snowy-json
[5]: https://github.com/everit-org/json-schema
[6]: https://github.com/leadpony/justify
[7]: https://github.com/worldturner/medeia-validator
[8]: https://github.com/erosb/json-sKema
[JSON-Schema-Test-Suite]: https://github.com/json-schema-org/JSON-Schema-Test-Suite
[jhm]: https://github.com/openjdk/jmh
[confluent]: https://www.confluent.io/
[functionalTestRuns]: https://github.com/creek-service/json-schema-validation-comparison/actions/workflows/run-func-test.yml
[performanceBenchmarkRuns]: https://github.com/creek-service/json-schema-validation-comparison/actions/workflows/run-perf-test.yml
