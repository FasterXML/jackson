#!/bin/sh

java -Xmx48m -server -cp build/classes/perf:build/classes/core\
:build/classes/mapper:lib/perf/\* \
 TestJsonPerf \
 $*
