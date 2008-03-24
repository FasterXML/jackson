#!/bin/sh

java -Xmx48m -server\
 -cp lib/junit/junit-3.8.1.jar\
:build/classes\
:test/classes\
 $*
