#!/usr/bin/env python3
"""Summarize the OTLP JSON lines written by the harness sink.

usage: summarize.py build/harness/<setup>.jsonl > summary.json

For each span (scope, name) and each log record (scope, event name) it records the attribute
keys with their value types, so two setups can be compared with diff.py.
"""
import json
import sys
from collections import defaultdict


def vtype(v):
    for k in ("stringValue", "intValue", "doubleValue", "boolValue", "arrayValue", "kvlistValue", "bytesValue"):
        if k in v:
            if k == "arrayValue":
                vals = v[k].get("values", [])
                return "array<%s>" % (vtype(vals[0]) if vals else "empty")
            return k.replace("Value", "")
    return "empty"


def keys(attrs):
    return {a["key"]: vtype(a["value"]) for a in attrs or []}


def main(path):
    out = {"resource": {}, "records": {}, "headers": {}, "paths": defaultdict(int)}
    records = defaultdict(lambda: {"count": 0, "attrs": {}, "extra": {}})
    for line in open(path):
        line = line.strip()
        if not line:
            continue
        d = json.loads(line)
        out["paths"][d["path"]] += 1
        for k, v in d["headers"].items():
            if k.startswith("x-"):
                out["headers"][k] = v if k != "x-honeycomb-team" else "<fake>"
        body = d["body"]
        for rs in body.get("resourceSpans", []):
            out["resource"].update(keys(rs["resource"].get("attributes")))
            for ss in rs.get("scopeSpans", []):
                for s in ss["spans"]:
                    r = records["span|%s|%s" % (ss["scope"]["name"], s["name"])]
                    r["count"] += 1
                    r["attrs"].update(keys(s.get("attributes")))
                    r["extra"]["has_parent"] = bool(s.get("parentSpanId"))
                    r["extra"]["status"] = s.get("status", {}).get("code", "UNSET")
                    r["extra"]["duration_ns"] = int(s["endTimeUnixNano"]) - int(s["startTimeUnixNano"])
                    r["extra"]["span_events"] = sorted({e["name"] for e in s.get("events", [])})
        for rl in body.get("resourceLogs", []):
            out["resource"].update(keys(rl["resource"].get("attributes")))
            for sl in rl.get("scopeLogs", []):
                for l in sl["logRecords"]:
                    attrs = keys(l.get("attributes"))
                    name = l.get("eventName") or "(attr event.name=%s)" % next(
                        (a["value"].get("stringValue") for a in l.get("attributes", []) if a["key"] == "event.name"), "none"
                    )
                    r = records["log|%s|%s" % (sl["scope"]["name"], name)]
                    r["count"] += 1
                    r["attrs"].update(attrs)
                    r["extra"]["severity"] = l.get("severityText", "")
                    r["extra"]["has_body"] = "body" in l
                    r["extra"]["has_eventName_field"] = bool(l.get("eventName"))
        for rm in body.get("resourceMetrics", []):
            out["resource"].update(keys(rm["resource"].get("attributes")))
            for sm in rm.get("scopeMetrics", []):
                for m in sm["metrics"]:
                    r = records["metric|%s|%s" % (sm["scope"]["name"], m["name"])]
                    r["count"] += 1
    out["records"] = records
    out["paths"] = dict(out["paths"])
    json.dump(out, sys.stdout, indent=1, sort_keys=True)


main(sys.argv[1])
