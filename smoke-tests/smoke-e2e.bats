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

@test "SDK can send spans" {
  result=$(span_names_for ${SMOKE_TEST_SCOPE})
  assert_equal "$result" '"test-span"'
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

