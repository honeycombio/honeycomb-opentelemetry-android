#!/usr/bin/env bats

load test_helpers/utilities

CONTAINER_NAME="android-test"
SMOKE_TEST_SCOPE="@honeycombio/smoke-test"

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
"telemetry.sdk.language"
"telemetry.sdk.name"
"telemetry.sdk.version"'

  result=$(resource_attribute_named "telemetry.sdk.language" "string" | uniq)
  assert_equal "$result" '"android"'
}

@test "SDK can send spans" {
  result=$(span_names_for ${SMOKE_TEST_SCOPE})
  assert_equal "$result" '"test-span"'

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
    assert_not_empty $(spans_on_view_named "@honeycombio/instrumentation-ui" "Touch Began" "example_button")
    assert_not_empty $(spans_on_view_named "@honeycombio/instrumentation-ui" "Touch Ended" "example_button")
}

@test "UI click events are captured" {
    assert_not_empty $(spans_on_view_named "@honeycombio/instrumentation-ui" "click" "example_button")
}

@test "UI touch events have all attributes" {
    span=$(spans_on_view_named "@honeycombio/instrumentation-ui" "click" "example_button")

    name=$(echo "$span" | jq '.attributes[] | select(.key == "view.name").value.stringValue')
    assert_equal "$name" '"example_button"'

    class=$(echo "$span" | jq '.attributes[] | select(.key == "view.class").value.stringValue')
    assert_equal "$class" '"android.widget.Button"'

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
  result=$(span_names_for "io.honeycomb.render-instrumentation" | sort | uniq -c | tr -s ' ')
  assert_equal "$result" ' 7 "View Body"
 7 "View Render"'

  # the View Render spans are tracking the views we expect
  total_duration=$(attribute_for_span_key "io.honeycomb.render-instrumentation" "View Render" "view.name" string | sort | tr -s ' ')
  assert_equal "$total_duration" '"expensive text 1"
"expensive text 2"
"expensive text 3"
"expensive text 4"
"main view"
"nested expensive text"
"nested expensive view"'
}
