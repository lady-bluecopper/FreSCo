# FreSCo
Frequent Pattern Mining in Simplicial Complexes

## Content

	scripts/run.sh .......... script for running the algorithm
	scripts/config.cfg ...... configuration file
	data/ ....... datasets used in the paper
	src/ ......... source files

## Dataset Format

The input file must be a space separated list of integers, where each integer represents a vertex and each line represents a simplex in the complex.
The algorithm does not assume that all the simplices in the complex are maximal.

## Requirements

	Java JRE v1.8.0

## Usage
You can use FreSCo either by running the script run.sh included in this package or by running the following command:

	java -cp FreSCo.jar:lib/* fsminer.Main dataFolder=<input_data> outputFolder=<output_data> dataFile=<file_name> minFreq=<frequency_threshold> allMatches=<whether_you_want_exact_frequencies> supportMeasure=mni harmful=false minSize=<min_dimension> maxSize=<max_size> limited=false timeout=6000 storeOccMap=false

### Using the Script
The value of each parameter used by FreSCo must be set in the configuration file config.cfg:

General settings:

- input_data: path to the folder containing the graph file.
- output_data: path to the folder to store the results.
- maxSize: max dimensionality of the simplets to mine.
- allMatches: whether you want exact frequencies or minimum frequencies.
- supportMeasure: 'mni' computes the MNI-based support, 'mis' computes the maximum-independent-set-based support.
- harmful: if true and if supportMeasure is 'mis', it computes the overlap-graph-based support.
- limited: whether you want to save memory during the computation or not.
- timeout: maximum number of milliseconds you want to spend on the examination of a candidate match.
- storeOccMap: whether you want to store the image sets of the vertices or not.

Dataset-related settings:

- Dataset names: names of the files.
- Default values: comma-separated list of default values and information about the datasets, i.e., default frequency threshold, default min dimension, and default max size.
- Frequencies: comma-separated list of frequency thresholds.
- MinSizes: comma-separated list of min dimensionality values.
- Experimental flags: test to perform among (1) test multiple min dimensionality values with default frequency threshold, (2) test multiple frequency thresholds with default minSize.

Then, the arrays that store the names, the frequencies, the sample sizes, and the experimental flags of each dataset to test must be declared at the beginning of the script run.sh. 
