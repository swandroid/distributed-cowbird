if [ $# -ne 4 ]; then
    echo usage: ./start_sources.sh [number-of-sources] [path-to-source..] [source-frequency] [front-end-ip-address]
    exit 1
fi


number_of_sources=$1
path_to_source=$2
source_frequency=$3
front_end_ip=$4

for number in $(seq 1 $number_of_sources)
do
        java -cp $path_to_source SensorApp 10 $source_frequency $front_end_ip &
done
