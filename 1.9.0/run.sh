#!/bin/sh

java -Xmx128m -server\
 -XX:-PrintGC -XX:-PrintGCDetails \
 -cp lib/junit/junit-3.8.1.jar\
:lib/repackaged/\*:lib/xml/\*\
:build/classes/core:build/classes/mapper:build/classes/xc:build/classes/extra\
:build/classes/perf:build/classes/smile \
 $*
