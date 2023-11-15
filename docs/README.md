# Repos GitHub pages site.

## Setup

If you want to hack about with the site or add content, then follow these instructions to be able to run locally.

### Prerequisites

1. Install Git, obviously.
2. [Install Jekyll](https://jekyllrb.com/docs/installation)
3. Install [Builder](https://bundler.io/) by running `gem install bundler`.

### Installing

#### 1. Install the gems

```shell
(cd docs && bundle install)
```

#### 2. Update

Occasionally update gems

```shell
git checkout main
git pull
(cd docs && bundle update)
git checkout -b gems-update
git add .
git commit -m "updating gems"
git push --set-upstream origin gems-update
```

#### 3. Generate includes

For the site to render correctly certain include files need to be generated.

Includes are stored in the `docs/_includes` directory and will be ignored by git.

These include:

| Include details                                                           | Gradle task name                       | Filename                |
|---------------------------------------------------------------------------|----------------------------------------|-------------------------|
| A JSON document containing the details of all implementations under test. | extractImplementations                 | implementations.json    |
| A JSON document containing the summary of the functional testing          | runFunctionalTests                     | functional-summary.json |
| A Markdown document containing the per-draft functional testing results   | runFunctionalTests                     | per-draft.md            |
| A JSON document containing the results of the performance benchmarking    | runBenchmarkSmokeTest or runBenchmarks | benchmark_results.json  |

Generate these locally by running:

```shell
./graldew buildTestIncludes
```

Note: this will not run the full performance benchmarking as this takes many hours. 
Instead, it will run the smoke benchmarks will generate inaccurate data go enough for testing the rendering of the website. 

#### 4. Run the local server

```shell
(cd docs && bundle exec jekyll serve --livereload --baseurl /json-schema-validation-comparison)
```

This will launch a web server so that you can work on the site locally.
Check it out on [http://localhost:4000/json-schema-validation-comparison](http://localhost:4000/json-schema-validation-comparison).