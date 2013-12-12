#!/bin/bash
#
# Restart processing of a batch either from the first error or from a specific event.
#
# Author: jrg, ktc, csr
#

# Use null-globbing so * can expand to empty and we can avoid nasty surprises
shopt -s nullglob

SCRIPT_PATH=$(dirname $(readlink -f $0))

##trigger_file=transfer_acknowledged


print_usage()
{
    echo "Usage: $(basename $0) -c <config_file> -b <batch_number> -n <round_trip_number> [-e <event_name>]"
    echo "-c    Path to configuration file"
    echo "-b    The number of the batch to be reprocessed"
    echo "-n    The number of the round-trip to be reprocessed"
    echo "-e    The name of the event at which to start reprocessing"
    echo
}

# Let's get this party started
if [ $# -gt 0 ]; then
    while getopts c:b:n:e: opt
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

# Check that we got the args we wanted
[ -z "$config" ] && print_usage && exit 2
[ -z "$batchId" ] && print_usage && exit 3
[ -z "$roundTrip" ] && print_usage && exit 4

if [ -z "$config" ]; then
	echo "config not received" >&2
	echo "usage: $(basename $0) /path/to/config.sh" >&2
	exit 1
fi

source "$config"

# Check that mandatory settings are atleast defined
for var in url_to_doms doms_username doms_password url_to_pid_gen max_attempts wait_time
do
    [ -z "${!var}" ] && echo "ERROR: $config must define \$$var" && exit 5
done




java -classpath $SCRIPT_PATH/../conf/:$SCRIPT_PATH/../lib/'*' dk.statsbiblioteket.medieplatform.autonomous.newspaper.RestartWorkflow "$batch_id" "$roundtrip" "$url_to_doms" "$doms_username" "$doms_password" "$url_to_pid_gen" "$max_attempts" "$wait_time" "$event"


