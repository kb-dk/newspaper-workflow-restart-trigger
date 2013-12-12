#!/bin/bash
#
# Config file for restart-workflow.sh
#

# Fedora location
url_to_doms='${fedora.server}'

# Username for calls to DOMS
doms_username='${fedora.admin.username}'

# Password for calls to DOMS
doms_password='${fedora.admin.password}'

# Location of PID generator
url_to_pid_gen='${pidgenerator.location}'

max_attempts=10
wait_time=100L
