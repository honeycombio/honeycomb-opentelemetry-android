#!/usr/bin/env python3
"""Compare two summaries made by summarize.py.

usage: diff.py a.json b.json [--map "scopeA|nameA=scopeB|nameB" ...]

Prints resource-key differences, records only on one side, and, for records on both sides,
attribute keys (or key types) that differ. --map pairs records whose names differ between the
two sides (for example a log event on one side and a span on the other).
"""
import json
import sys

IGNORE = {"session.id"}


def load(p):
    return json.load(open(p))


def main():
    args = sys.argv[1:]
    maps = {}
    while "--map" in args:
        i = args.index("--map")
        a, b = args[i + 1].split("=")
        maps[a] = b
        del args[i : i + 2]
    a, b = load(args[0]), load(args[1])
    ra, rb = set(a["resource"]), set(b["resource"])
    print("== resource keys only in A:", sorted(ra - rb))
    print("== resource keys only in B:", sorted(rb - ra))
    for k in sorted(set(ra & rb)):
        if a["resource"][k] != b["resource"][k]:
            print("   resource type differs", k, a["resource"][k], b["resource"][k])
    A, B = a["records"], b["records"]
    mapped = {k: v for k, v in maps.items() if k in A and v in B}
    onlyA = sorted(k for k in A if k not in B and k not in mapped)
    onlyB = sorted(k for k in B if k not in A and k not in mapped.values())
    print("== records only in A (%s):" % args[0])
    for k in onlyA:
        print("   ", k, "x%d" % A[k]["count"])
    print("== records only in B (%s):" % args[1])
    for k in onlyB:
        print("   ", k, "x%d" % B[k]["count"])
    pairs = [(k, k) for k in sorted(set(A) & set(B))] + list(mapped.items())
    print("== records in both:")
    for ka, kb in pairs:
        ka_attrs, kb_attrs = A[ka]["attrs"], B[kb]["attrs"]
        oa = sorted(set(ka_attrs) - set(kb_attrs) - IGNORE)
        ob = sorted(set(kb_attrs) - set(ka_attrs) - IGNORE)
        td = sorted(k for k in set(ka_attrs) & set(kb_attrs) if ka_attrs[k] != kb_attrs[k])
        ea, eb = A[ka]["extra"], B[kb]["extra"]
        xd = {k: (ea[k], eb[k]) for k in ea if k in eb and ea[k] != eb[k] and k != "duration_ns"}
        label = ka if ka == kb else "%s  ->  %s" % (ka, kb)
        if oa or ob or td or xd:
            print("  DIFF", label)
            if oa:
                print("      only in A:", oa)
            if ob:
                print("      only in B:", ob)
            if td:
                print("      type differs:", [(k, ka_attrs[k], kb_attrs[k]) for k in td])
            if xd:
                print("      other:", xd)
        else:
            print("  same", label, "(%d attrs)" % len(ka_attrs))


main()
