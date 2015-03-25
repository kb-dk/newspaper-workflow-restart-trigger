#!/bin/sh
#
# Prioritize a specific roundtrip
#

SCRIPT_PATH=$(dirname $(readlink -f $0))

config=$(dirname $SCRIPT_PATH)/conf/config.properties

print_usage()
{
    echo "Usage: $(basename $0) -b <batch_number> -n <round_trip_number>  [-c <config_file>]  [-p <priority>]"
    echo '-b    The Batch ID for which to add an event'
    echo '-n    The number of the round-trip for which to add an event'
    echo '-c    Path to configuration (java properties) file'
    echo '-p    The priority of the event 1-9, 1 highest'
    echo
}

# Pick up arguments
if [ $# -gt 0 ]; then
    while getopts c:b:n:m:s:p: opt
    do
        case $opt in
            c)
               config=$OPTARG
               ;;
            b)
                batchId=$OPTARG
                ;;
            n)
                roundTrip=$OPTARG
                ;;
            p)
                priority=$OPTARG
                ;;
            \?)
                print_usage
                exit 1
                ;;
        esac
    done
    shift `expr $OPTIND - 1`
else
    print_usage
    exit 1
fi

# Check that we got the required arguments
[ -z "$config" ] && print_usage && exit 2
[ -z "$batchId" ] && print_usage && exit 3
[ -z "$roundTrip" ] && print_usage && exit 4
[ -z "$priority" ] && print_usage && exit 5

$JAVA_HOME/bin/java -classpath $SCRIPT_PATH/../conf/:$SCRIPT_PATH/../lib/'*' dk.statsbiblioteket.medieplatform.autonomous.newspaper.RestartWorkflow \
prioritize "$config" "$batchId" "$roundTrip" "$priority"
