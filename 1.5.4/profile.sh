#!/bin/sh

java -Xmx16m -server \
 -XX:CompileThreshold=2000 \
 -cp build/classes/core:build/classes/mapper:build/classes/extra:build/classes/perf\
 -Xrunhprof:cpu=samples,depth=10,verbose=n,interval=2 \
$*
