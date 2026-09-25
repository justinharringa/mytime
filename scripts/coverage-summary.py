#!/usr/bin/env python3
"""Prints a Markdown line/branch coverage table from JaCoCo XML reports.

Usage: scripts/coverage-summary.py <module>=<report.xml> ...
CI appends the output to $GITHUB_STEP_SUMMARY.
"""

import sys
import xml.etree.ElementTree as ET


def percent(counters, kind):
    covered, missed = counters.get(kind, (0, 0))
    total = covered + missed
    return f"{100 * covered / total:.0f}% ({covered}/{total})" if total else "n/a"


def main(args):
    print("| Module | Lines | Branches |")
    print("|---|---|---|")
    for arg in args:
        module, path = arg.split("=", 1)
        report = ET.parse(path).getroot()
        counters = {
            counter.get("type"): (int(counter.get("covered")), int(counter.get("missed")))
            for counter in report.findall("counter")
        }
        print(f"| {module} | {percent(counters, 'LINE')} | {percent(counters, 'BRANCH')} |")


if __name__ == "__main__":
    main(sys.argv[1:])
