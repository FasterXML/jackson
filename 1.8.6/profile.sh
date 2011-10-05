#!/bin/sh

java -Xmx64m -server \
 -cp build/classes/core:build/classes/mapper:build/classes/extra\
:build/classes/smile:build/classes/xc:build/classes/perf\
:lib/xml/\*\
 -Xrunhprof:cpu=samples,depth=10,verbose=n,interval=2 \
$*
