#!/bin/sh

java -Xmx48m -server\
 -cp build/classes\
:lib/perf/json-org.jar\
:lib/perf/stringtree-json-2.0.5.jar\
:lib/perf/antlr-2.7.6.jar\
:lib/perf/jsontools-core-1.5.jar\
:lib/perf/noggit.jar\
 TestJsonPerf \
 $*
