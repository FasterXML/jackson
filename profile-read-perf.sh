#!/bin/sh

java -Xmx16m -server \
-cp build/classes/perf:perflib/\* \
-XX:CompileThreshold=2000 \
-Xrunhprof:cpu=samples,depth=10,verbose=n,interval=2 \
TestReadPerf $*
