#!/bin/sh

java -Xmx16m -server \
-cp build/classes \
-XX:CompileThreshold=2000 \
-Xrunhprof:cpu=samples,depth=10,verbose=n,interval=2 \
$*
