This repository have been archived and exists for historical purposes. 
No updates or futher development will go into this repository. The content can be used as is but no support will be given. 

---

newspaper-workflow-restart-trigger
==================================

Command-line utility to force restart of the workflow for a given batch-round-trip


#Rationale#

The ingest workflow is dependent on many external systems in order to function correctly, for example

* DOMS
* MfPak
* Hadoop
* Bitmagasin
* SBOI etc.

If any of these systems malfunctions during ingest then one or more components will fail and any components dependent on
these will fail to start.

Another scenario is where a bug in one of the components causes it to fail and a new and hopefully improved version of
the software has been deployed.

In each of these cases, one will wish to restart the workflow. Specifically, the workflow needs to be restarted from the
chronologically earliest component which failed.

In rarer cases, such as where a bug caused a component to give a spuriously successful result, one may also need to be
able to restart the workflow from an arbitrary position.

This package provides a command-line tool capable of handling these scenarios.

#Packaging And Installation#

The release consists of a single .tar.gz archive which unpacks to a directory containg `lib`, `conf` and `bin` subdirectories. There
is a single script `restart-workflow.sh` in the `bin` directory. Running this without any parameters will output a usage summary. The `conf`
directory contains a sample properties file and logback configuration, and all dependencies are in the `lib` directory.

The applications has a negligible I/O, CPU and diskspace footprint and requires access only to DOMS/fedora to function.


#Usage#

###Restart From First Error###

This is the commonest case. Using the script provided, the tool can be run from the command line with

    ./restart_workflow.sh -c <configFile> -b <batchId> -n <roundTripNumber>
This has the effect of finding the earliest failed component for the given batch/roundtrip combination and removing it and
all subsequent events from the workflow history. This will then trigger the workflow system to restart processing the batch
from that point.

Running

    ./restart_workflow.sh
without any arguments will detail the other parameters it can take.

###Restart From A Specific Event###

This should be a less common case and will normally require consulting with the system developers to determine the exact
procedure for a given scenario. In this case the script takes an extra option as follows

    ./restart_workflow.sh -c <configFile> -b <batchId> -n <roundTripNumber> -e <eventName>
the eventName parameter is the internal name by which a run of any autonomous component is identified in DOMS. When the
restart_workflow.sh script is run with this parameter then the specified component and all subsequent components are
removed from the workflow history, triggering a restart.

###Maintaining Integrity###
In reality, this component does not actually delete information from DOMS. Instead a backup of the events for the given
batch/roundtrip is made and stored as an additional datastream in DOMS in case it is ever needed.

###Configuration File###
The configuration file is a java properties file specifying the connection information for DOMS as follows:

    # The RestartWorkflow tool needs only config-parameters for connecting to DOMS
    #
    doms.username=fedoraAdmin
    doms.password=****
    doms.url=http://foobar/fedora
    doms.pidgenerator.url=http://foobar/pidgenerator-service



