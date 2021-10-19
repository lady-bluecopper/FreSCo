#!/bin/bash

# Loading configurations for experiments
echo '>> Loading config file config.cfg'
source config.cfg

unset datasets
declare -A datasets
datasets[$enron_db]=$enron_defaults
datasets[$etfs_db]=$etfs_defaults
datasets[$zebra_db]=$zebra_defaults
datasets[$dblp_db]=$dblp_defaults

unset test_freqs
declare -A test_freqs
test_freqs[$enron_db]=$enron_freqs
test_freqs[$etfs_db]=$etfs_freqs
test_freqs[$zebra_db]=$zebra_freqs
test_freqs[$dblp_db]=$dblp_freqs

unset test_sizes
declare -A test_sizes
test_sizes[$enron_db]=$enron_sizes
test_sizes[$etfs_db]=$etfs_sizes
test_sizes[$zebra_db]=$zebra_sizes
test_sizes[$dblp_db]=$dblp_sizes

unset flags
declare -A flags
flags[$enron_db]=$enron_flags
flags[$etfs_db]=$etfs_flags
flags[$zebra_db]=$zebra_flags
flags[$dblp_db]=$dblp_flags

echo -e '\n\n>> Creating directories ...'
mkdir -p $output_data

for dataset in ${!datasets[@]}
do
	dataset_path="$input_data"
	default=${datasets[${dataset}]}
	flag=${flags[${dataset}]}
	defaults=(`echo $default|tr "," "\n"`)
	experiments=(`echo $flag|tr "," "\n"`)

	echo ">> Processing dataset ${dataset} with default values (${defaults[@]})"
	echo ">> Experiment flags ${experiments[@]}"

	if [[ ${experiments[0]} -eq "1" ]]; then
		echo '-----------------------------'
		echo '     Varying Min Size 	   '
		echo '-----------------------------'

		sizes=(`echo ${test_sizes[${dataset}]}|tr "," "\n"`)
		for s in ${sizes[*]}
		do
			echo "Running command ..."
			echo "$JVM $FSMINER_jar dataFolder=${input_data} outputFolder=${output_data} dataFile=${dataset} minFreq=${defaults[0]} allMatches=$allMatches supportMeasure=$supportMeasure harmful=$harmful minSize=$s maxSize=${defaults[2]} limited=$limited timeout=$timeout storeOccMap=$storeOccMap"
			echo "---- `date`"
			$JVM $FSMINER_jar dataFolder=${input_data} outputFolder=${output_data} dataFile=${dataset} minFreq=${defaults[0]} allMatches=$allMatches supportMeasure=$supportMeasure harmful=$harmful minSize=$s maxSize=${defaults[2]} limited=$limited timeout=$timeout storeOccMap=$storeOccMap
		done
	fi

	if [[ ${experiments[1]} -eq "1" ]]; then
		echo '-----------------------------'
		echo '      Varying Frequency 	   '
		echo '-----------------------------'

		freqs=(`echo ${test_freqs[${dataset}]}|tr "," "\n"`)
		for freq in ${freqs[*]}
		do
			echo "Running command ..."
			echo "$JVM $FSMINER_jar dataFolder=${input_data} outputFolder=${output_data} dataFile=${dataset} minFreq=$freq allMatches=$allMatches supportMeasure=$supportMeasure harmful=$harmful minSize=${defaults[1]} maxSize=${defaults[2]} limited=$limited timeout=$timeout storeOccMap=$storeOccMap"
			echo "---- `date`"
			$JVM $FSMINER_jar dataFolder=${input_data} outputFolder=${output_data} dataFile=${dataset} minFreq=$freq allMatches=$allMatches supportMeasure=$supportMeasure harmful=$harmful minSize=${defaults[1]} maxSize=${defaults[2]} limited=$limited timeout=$timeout storeOccMap=$storeOccMap
		done
	fi
done
echo 'Terminated.'
