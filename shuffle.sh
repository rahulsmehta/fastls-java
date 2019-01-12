#!/bin/bash

tail -n+2 $1 | sort -R > shuffled_edges
N=`head -n 1 ${1}`
FN=$2
echo $N > $FN
echo $N
cat shuffled_edges >> $FN
rm shuffled_edges
