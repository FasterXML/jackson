#!/bin/sh

java -Xmx16m -server \
 -XX:CompileThreshold=2000 \
 -cp build/classes/core:build/classes/mapper:build/classes/extra\
:build/classes/smile:build/classes/xc:build/classes/perf\
:lib/xml/\*\
 -Xrunhprof:cpu=samples,depth=10,verbose=n,interval=2 \
$*
