#!/bin/bash
echo "sleeping for 3 minutes"
sleep 180
pgrep java
pkill java
#finish sound
echo -en "\007"
echo -en "\007"
echo -en "\007"
echo -en "\007"
