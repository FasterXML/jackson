#!/bin/sh

java -Xmx16m -server -cp build/classes \
TestReadPerf $*
