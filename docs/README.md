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

#### 3. Run the local server

```shell
(cd docs && bundle exec jekyll serve --livereload --baseurl /json-schema-validation-comparison)
```

This will launch a web server so that you can work on the site locally.
Check it out on [http://localhost:4000/json-schema-validation-comparison](http://localhost:4000/json-schema-validation-comparison).
