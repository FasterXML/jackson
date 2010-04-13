#!/bin/sh

java -Xmx48m -server -cp build/classes \
 TestCopyPerf $*
