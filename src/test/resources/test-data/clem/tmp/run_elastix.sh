#!/bin/bash
ELASTIX_PATH=/Applications/elastix_macosx64_v4.8/
export DYLD_LIBRARY_PATH=$ELASTIX_PATH/lib/
$ELASTIX_PATH/bin/elastix $@
