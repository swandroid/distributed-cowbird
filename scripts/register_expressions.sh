#!/bin/bash


if [ $# -ne 2 ]; then
    echo usage: ./register_expressions.sh [number-of-expressions] [front-end-ip-address]
    exit 1
fi


number_of_expressions=$1
front_end_ip=$2

for number in $(seq 1 $number_of_expressions)
do
        curl $front_end_ip:9000/register/fog/expression
done
