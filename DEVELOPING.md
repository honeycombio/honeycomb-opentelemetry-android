# Local Development

## Prerequisites

**Required:**

- [Android Studio](https://developer.android.com/studio)

**Required for Smoke Tests:**

- [Android SDK Command-line Tools](https://developer.android.com/tools)
- [`bats-core`](https://bats-core.readthedocs.io/en/stable/) and [`jq`](https://jqlang.github.io/jq/)
- Docker & Docker Compose
  - [Docker Desktop](https://www.docker.com/products/docker-desktop/) is a reliable choice if you don't have your own preference.

## Smoke Tests

Smoke tests use Android Command-line Tools and Docker using `docker-compose`, exporting telemetry to a local collector.
Tests are run using `bats-core` and `jq`, bash tools to make assertions against the telemetry output.

Install `bats-core` and `jq` for local testing:

```sh
brew install bats-core
brew install jq
```

Smoke tests can be run with `make` targets (the latter works better in CI).

```sh
cd smoke-tests
make smoke
```

The results of both the tests themselves and the telemetry collected by the collector are in a file `data.json` in the `smoke-tests/collector/` directory.
These artifacts are also uploaded to Circle when run in CI.

After smoke tests are done, tear down docker containers:

```sh
make unsmoke
```
