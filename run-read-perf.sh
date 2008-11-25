#!/bin/sh

java -Xmx16m -server \
-cp build/perf-classes:perflib/\* \
TestReadPerf $*
