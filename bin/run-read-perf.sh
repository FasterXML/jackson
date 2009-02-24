#!/bin/sh

java -Xmx16m -server \
-cp build/classes/perf:perflib/\* \
TestReadPerf $*
