#!/usr/bin/env bats

load test_helpers/utilities

CONTAINER_NAME="android-test"
SMOKE_TEST_SCOPE="io.honeycomb.smoke-test"

setup_file() {
  echo "# 🚧 preparing test" >&3
}
teardown_file() {
  cp collector/data.json collector/data-results/data-${CONTAINER_NAME}.json
}

@test "SDK only installs instrumentation once" {
  result=$(sessions_started | wc -l | tr -d ' ')
  assert_equal "$result" "1"
}

@test "SDK sends correct resource attributes" {
  result=$(resource_attributes_received | sort | uniq)
  assert_equal "$result" '"honeycomb.distro.runtime_version"
"honeycomb.distro.version"
"service.name"
"service.version"
"telemetry.sdk.language"
"telemetry.sdk.name"
"telemetry.sdk.version"'

  result=$(resource_attribute_named "telemetry.sdk.language" "string" | uniq)
  assert_equal "$result" '"android"'
}

@test "SDK captures Activity Lifecycle events" {
    # This test is primarily to test that OTel integration is working, and the exact order of
    # events depends on the order the tests were run. So, just check that all types are present.

    result=$(attribute_for_span_key "io.opentelemetry.lifecycle" Created "activityName" "string" | sort | uniq)
    assert_equal "$result" '"ClassicActivity"
"MainActivity"'

    result=$(attribute_for_span_key "io.opentelemetry.lifecycle" Paused "activityName" "string" | sort | uniq)
    assert_equal "$result" '"ClassicActivity"
"MainActivity"'

    result=$(attribute_for_span_key "io.opentelemetry.lifecycle" Stopped "activityName" "string" | sort | uniq)
    assert_equal "$result" '"ClassicActivity"
"MainActivity"'

    result=$(attribute_for_span_key "io.opentelemetry.lifecycle" Destroyed "activityName" "string" | sort | uniq)
    assert_equal "$result" '"ClassicActivity"
"MainActivity"'
}

@test "SDK captures Fragment Lifecycle events" {
    # This test is primarily to test that OTel integration is working, and the exact order of
    # events depends on the order the tests were run. So, just check that all types are present.

    result=$(attribute_for_span_key "io.opentelemetry.lifecycle" Created "fragmentName" "string" | sort | uniq)
    assert_equal "$result" '"FirstFragment"
"SecondFragment"'

    result=$(attribute_for_span_key "io.opentelemetry.lifecycle" Paused "fragmentName" "string" | sort | uniq)
    assert_equal "$result" '"FirstFragment"
"SecondFragment"'

    result=$(attribute_for_span_key "io.opentelemetry.lifecycle" Stopped "fragmentName" "string" | sort | uniq)
    assert_equal "$result" '"FirstFragment"
"SecondFragment"'

    result=$(attribute_for_span_key "io.opentelemetry.lifecycle" Destroyed "fragmentName" "string" | sort | uniq)
    assert_equal "$result" '"FirstFragment"
"SecondFragment"'
}

@test "SDK can send spans" {
  result=$(span_names_for ${SMOKE_TEST_SCOPE})
  assert_equal "$result" '"test-span"'

  sampleRate=$(attribute_for_span_key ${SMOKE_TEST_SCOPE} "test-span" SampleRate "double")
  assert_equal "$sampleRate" '1'

  baggage=$(attribute_for_span_key ${SMOKE_TEST_SCOPE} "test-span" "baggage-key" "string")
  assert_equal "$baggage" '"baggage-value"'
}

@test "SDK can send metrics" {
  result=$(metric_names_for ${SMOKE_TEST_SCOPE})
  assert_equal "$result" '"smoke-test.metric.int"'
}

@test "SDK detects ANRs" {
  result=$(unique_span_names_for "io.opentelemetry.anr")
  assert_equal "$result" '"ANR"'
}

@test "SDK can log manual exceptions" {
  result=$(attribute_for_log_key "io.honeycomb.crash" "event.name" "string")
  assert_equal "$result" '"device.crash"'

  result=$(attribute_for_log_key "io.honeycomb.crash" "exception.type" "string")
  assert_equal "$result" '"java.lang.RuntimeException"'

  result=$(attribute_for_log_key "io.honeycomb.crash" "exception.message" "string")
  assert_equal "$result" '"This exception was intentional."'

  result=$(attribute_for_log_key "io.honeycomb.crash" "exception.stacktrace" "string" \
    | grep "example.CorePlaygroundKt.onLogException")
  assert_not_empty "$result"
}

@test "SDK detects slow renders" {
  result=$(unique_span_names_for "io.opentelemetry.slow-rendering")
  assert_equal "$result" '"frozenRenders"
"slowRenders"'
}

@test "Network auto-instrumentation sends spans" {
  result=$(span_names_for "io.opentelemetry.okhttp-3.0")
  assert_equal "$result" '"GET"'
}

@test "UI touch events are captured" {
    assert_not_empty $(spans_on_view_named "io.honeycomb.ui" "Touch Began" "example_button")
    assert_not_empty $(spans_on_view_named "io.honeycomb.ui" "Touch Ended" "example_button")
}

@test "UI click events are captured" {
    assert_not_empty $(spans_on_view_named "io.honeycomb.ui" "click" "example_button")
}

@test "UI touch events have all attributes" {
    span=$(spans_on_view_named "io.honeycomb.ui" "click" "example_button")

    name=$(echo "$span" | jq '.attributes[] | select(.key == "view.name").value.stringValue')
    assert_equal "$name" '"example_button"'

    class=$(echo "$span" | jq '.attributes[] | select(.key == "view.class").value.stringValue')
    assert_equal "$class" '"androidx.appcompat.widget.AppCompatButton"'

    class=$(echo "$span" | jq '.attributes[] | select(.key == "view.accessibilityClassName").value.stringValue')
    assert_equal "$class" '"android.widget.Button"'

    text=$(echo "$span" | jq '.attributes[] | select(.key == "view.text").value.stringValue')
    assert_equal "$text" '"Example Button"'

    package=$(echo "$span" | jq '.attributes[] | select(.key == "view.id.package").value.stringValue')
    assert_equal "$package" '"io.honeycomb.opentelemetry.android.example"'

    entry=$(echo "$span" | jq '.attributes[] | select(.key == "view.id.entry").value.stringValue')
    assert_equal "$entry" '"example_button"'
}

@test "Render Instrumentation attributes are correct" {
  # we got the spans we expect
  result=$(span_names_for "io.honeycomb.view" | sort | uniq -c | tr -s ' ')
  assert_equal "$result" ' 7 "View Body"
 7 "View Render"'

  # the View Render spans are tracking the views we expect
  total_duration=$(attribute_for_span_key "io.honeycomb.view" "View Render" "view.name" string | sort | tr -s ' ')
  assert_equal "$total_duration" '"expensive text 1"
"expensive text 2"
"expensive text 3"
"expensive text 4"
"main view"
"nested expensive text"
"nested expensive view"'
}

@test "Span Processor gets added correctly" {
    result=$(spans_received | jq ".attributes[] | select (.key == \"app.metadata\").value.stringValue" "app.metadata" string | uniq)
}
