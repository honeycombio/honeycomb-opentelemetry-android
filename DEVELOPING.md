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

## Testing against Android 11's `java.time.Duration`

Unit tests run on a desktop JDK, whose `java.time.Duration` has methods that older Android versions lack.
For example, `Duration.toSeconds()` does not exist before Android 12 (API 31), and calling it throws `NoSuchMethodError`.
Android 11's `Duration` matches Java 8's, so to catch that kind of bug, run the unit tests with Java 8's `Duration` in place of the JDK's:

```sh
# JDK_8 is the path to any JDK 8 installation
mkdir -p /tmp/android11-duration
unzip -qo "$JDK_8/jre/lib/rt.jar" 'java/time/Duration*.class' -d /tmp/android11-duration
./gradlew :core:testDebugUnitTest -Pandroid11Duration=/tmp/android11-duration
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
