# Local Development

## Prerequisites

**Required:**

- [Android Studio](https://developer.android.com/studio)

```sh
# from the top-level directory
make build
make test
make clean
```

## Smoke Tests

Smoke tests use Android Command-line Tools and Docker using `docker-compose`, exporting telemetry to a local collector.
Tests are run using `bats-core` and `jq`, bash tools to make assertions against the telemetry output.

**Required for Smoke Tests:**

- [Android SDK Command-line Tools](https://developer.android.com/tools)
- [`bats-core`](https://bats-core.readthedocs.io/en/stable/) and [`jq`](https://jqlang.github.io/jq/)
- Docker & Docker Compose
  - [Docker Desktop](https://www.docker.com/products/docker-desktop/) is a reliable choice if you don't have your own preference.

**Android SDK Setup**

After installing Android Studio, set `ANDROID_HOME` to the location of your Android SDK, which is usually something like `$HOME/Library/Android/sdk`.

```sh
export ANDROID_HOME="$HOME/Library/Android/sdk"
```

If you don't already have Java, no need to install it separately: Android Studio includes it.

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

To install Command-line Tools go to Tools > SDK Manager > Android SDK > SDK Tools.


Install `bats-core` and `jq` for local testing:

```sh
brew install bats-core
brew install jq
```

Smoke tests can be run with `make` targets.

```sh
make smoke
```

The results of both the tests themselves and the telemetry collected by the collector are in a file `data.json` in the `smoke-tests/collector/` directory.

After smoke tests are done, tear down docker containers:

```sh
make unsmoke
```
