#!/bin/sh
#
# Remove specified event
#

SCRIPT_PATH=$(dirname $(readlink -f $0))

config=$(dirname $SCRIPT_PATH)/conf/config.properties
