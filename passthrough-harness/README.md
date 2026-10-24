# Pass-through harness (experiment, not for merge)

Runs one scenario on the JVM with Robolectric against a local OTLP/HTTP sink on loopback and
writes what reached the sink to `build/harness/<setup>.jsonl`. It uses a fake API key.

The scenario starts the SDK, launches an Activity with a Fragment, taps a button, emits a manual
span / log / metric (with baggage), logs a handled exception, runs the installed uncaught
exception handler, blocks the main thread long enough for the ANR watcher to report, and shuts
the SDK down.

```sh
./gradlew :passthrough-harness:testDebugUnitTest -Psetup=current
./gradlew :passthrough-harness:testDebugUnitTest -Psetup=upstream -PwithClicks
./gradlew :passthrough-harness:testDebugUnitTest -Psetup=shaped   -PwithClicks
```

| setup      | what it runs                                                                 |
|------------|------------------------------------------------------------------------------|
| `current`  | `Honeycomb.configure`, as shipped today                                     |
| `upstream` | stock `OpenTelemetryRumInitializer` from `android-agent`, nothing of ours    |
| `shaped`   | `Honeycomb.configurePassthrough`: upstream instrumentation plus processors   |

`-PwithClicks` puts upstream's `view-click` instrumentation on the classpath, `-Panr=false` skips
the 14 second ANR wait. Summarize and compare:

```sh
python3 passthrough-harness/summarize.py passthrough-harness/build/harness/current.jsonl > current.json
python3 passthrough-harness/summarize.py passthrough-harness/build/harness/shaped.jsonl  > shaped.json
python3 passthrough-harness/diff.py current.json shaped.json
```

The baseline (`current` on upstream 1.0.1, with the `:interaction` module) can be reproduced at
the first commit of this branch with `-PwithInteraction`.

Not covered: slow-rendering (Robolectric produces no frames), OkHttp (needs the ByteBuddy build
plugin), Compose, native crashes, and anything that needs a real device.
