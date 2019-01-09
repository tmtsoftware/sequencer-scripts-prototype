#!/usr/bin/env bash

if [ "$#" -ne 0 ]
then
mkdir -p ../../target/coursier/stage/"$1"/bin
cp coursier ../../target/coursier/stage/"$1"/bin

./coursier bootstrap -r jitpack com.github.tmtsoftware::sequencer-scripts:$1 -M Main -f -o ../../target/coursier/stage/"$1"/bin/sequencer-app

echo "Artifacts successfully generated"
else
echo "[ERROR] Provide version ID as argument"
fi
