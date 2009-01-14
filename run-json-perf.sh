#!/bin/sh

java -Xmx48m -server -cp build/perf-classes:build/classes:lib/perf/\* \
 TestJsonPerf \
 $*
