# UTILITY FUNCS

# Spans on a particular view.
# Arguments:
#   $1 - scope
#   $2 - span name
#   $3 - view.name
spans_on_view_named() {
    spans_received | jq ".scopeSpans[] \
        | select(.scope.name == \"$1\").spans[] \
        | select (.name == \"$2\") as \$span \
        | .attributes?[]? \
        | select (.key? == \"view.name\" and .value.stringValue == \"$3\") \
        | \$span"
}

# Span names for a given scope
# Arguments: $1 - scope name
span_names_for() {
	spans_from_scope_named $1 | jq '.name'
}

# Unique span names for a given scope
# Arguments: $1 - scope name
unique_span_names_for() {
	span_names_for $1 | sort | uniq
}

# Attributes for a given scope
# Arguments: $1 - scope name
span_attributes_for() {
	spans_from_scope_named $1 | \
		jq ".attributes[]"
}

# All resource attributes
resource_attributes_received() {
	spans_received | jq ".resource.attributes[]?"
}

# Spans for a given scope
# Arguments: $1 - scope name
spans_from_scope_named() {
	spans_received | jq ".scopeSpans[] | select(.scope.name == \"$1\").spans[]"
}

# All spans received
spans_received() {
	jq ".resourceSpans[]?" ./collector/data.json
}

metrics_received() {
  jq ".resourceMetrics[]?" ./collector/data.json
}

# Metrics for a given scope
# Arguments: $1 - scope name
metrics_from_scope_named() {
	metrics_received | jq ".scopeMetrics[] | select(.scope.name == \"$1\").metrics[]"
}

# Metric names for a given scope
# Arguments: $1 - scope name
metric_names_for() {
	metrics_from_scope_named $1 | jq '.name'
}


# ASSERTION HELPERS

# Fail and display details if the expected and actual values do not
# equal. Details include both values.
#
# Inspired by bats-assert * bats-support, but dramatically simplified
# Arguments:
# $1 - actual result
# $2 - expected result
assert_equal() {
	if [[ $1 != "$2" ]]; then
		{
			echo
			echo "-- 💥 values are not equal 💥 --"
			echo "expected : $2"
			echo "actual   : $1"
			echo "--"
			echo
		} >&2 # output error to STDERR
		return 1
	fi
}

# Fail and display details if the actual value is empty.
# Arguments: $1 - actual result
assert_not_empty_string() {
	EMPTY=(\"\")
	if [[ "$1" == "${EMPTY}" ]]; then
		{
			echo
			echo "-- 💥 value is empty 💥 --"
			echo "value : $1"
			echo "--"
			echo
		} >&2 # output error to STDERR
		return 1
	fi
}

# Fail and display details if the actual value is empty.
# Arguments: $1 - actual result
assert_not_empty() {
	if [[ "$1" == "" ]]; then
		{
			echo
			echo "-- 💥 value is empty 💥 --"
			echo "value : $1"
			echo "--"
			echo
		} >&2 # output error to STDERR
		return 1
	fi
}
