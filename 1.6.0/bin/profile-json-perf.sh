#!/bin/sh

java -Xmx48m -server -cp build/classes/perf:build/classes/core\
:build/classes/mapper:lib/perf/\* \
-XX:CompileThreshold=2000 \
-Xrunhprof:cpu=samples,depth=10,verbose=n,interval=2 \
 TestJsonPerf \
 $*
