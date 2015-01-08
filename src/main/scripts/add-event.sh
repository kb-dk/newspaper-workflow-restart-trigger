#!/bin/sh
#
# Add specified event
#

SCRIPT_PATH=$(dirname $(readlink -f $0))

config=$(dirname $SCRIPT_PATH)/conf/config.properties

print_usage()
{
    echo "Usage: $(basename $0) -b <batch_number> -n <round_trip_number>  [-c <config_file>] [-m <max_attempts>] [-s <max_wait>] [-e <event_name>]"
    echo '-b    The Batch ID for which to add an event'
    echo '-n    The number of the round-trip for which to add an event'
    echo '-c    Path to configuration (java properties) file'
    echo '-m    The maximum number of attempts (default 10)'
    echo '-s    The maximum wait (milliseconds) between attempts (default 1000)'
    echo '-e    The name of the event to add'
    echo
}

# Pick up arguments
if [ $# -gt 0 ]; then
    while getopts c:b:n:m:s:e: opt
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
            m)
                maxAttempts=$OPTARG
                ;;
            s)
                waitTime=$OPTARG
                ;;
            e)
                event=$OPTARG
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
[ -z "$event" ] && print_usage && exit 5
# Set defaults if not gotten
[ -z "$maxAttempts" ] && maxAttempts=10
[ -z "$waitTime" ] && waitTime=1000

$JAVA_HOME/bin/java -classpath $SCRIPT_PATH/../conf/:$SCRIPT_PATH/../lib/'*' dk.statsbiblioteket.medieplatform.autonomous.newspaper.RestartWorkflow \
add "$config" "$batchId" "$roundTrip" "$maxAttempts" "$waitTime" "$event"
