#!/bin/sh

java -Xmx48m -server\
 -cp lib/junit/junit-3.8.1.jar\
:lib/repackaged/\*\
:build/classes/core:build/classes/mapper:build/classes/xc:build/classes/extra\
:build/classes/perf:build/classes/smile \
 $*
