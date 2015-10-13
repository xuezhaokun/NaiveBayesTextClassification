!!!****this project does not include the data. If I include the all the data, the size of the zip file will be larger than the max allowbale size for provide system.****
How to setup:
	
	import the .zip file as existing archive project to workspace
	create a folder under the project called "data"
	under the "data/" folder create two subfolders one called "ibmmac" and the other called "sport"
	import all the ibmmac data to "data/ibmmac/" and import all the sport data to "data/sport"

How to get the results:
	
	When you run the NaiveBayes.java, it will output the instructions and results in console.
	If you want to run for different dataset, you need to manually change datePath variable in main function for different dataset.
	
	Copy and parse the results from console. And write them into .txt files under "results/rawdata"
	Then modify the data to separate ".dat" files and put them under "results/data_for_plots" folder for ploting in gnuplot
	And the plots figures from gnuplot are in "results/plots" folder
	
For analysis of the project, check report.pdf in results folder.