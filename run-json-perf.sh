#!/bin/sh

java -Xmx48m -server -cp build/perf-classes:build/classes/core\
:build/classes/mapper:lib/perf/\* \
 TestJsonPerf \
 $*
