#!/usr/bin/env bash

if [ "$#" -ne 0 ]
then
./coursier launch -r jitpack com.github.tmtsoftware::sequencer-scripts:$1 -M ocs.framework.SequencerApp -- "${@:2}"
fi
