#!/bin/bash

BASE="test_${1}.out"
HEIGHTS="${1}.heights"

echo $BASE

echo $HEIGHTS

./gradlew test > $BASE
cat $BASE | grep "LSTree" | awk '{print $8}' > $HEIGHTS
cat $BASE | grep "LookSelectImpl" | grep "Starting phase" | \
    tail -n 1 | awk '{print $NF}' >> $HEIGHTS
