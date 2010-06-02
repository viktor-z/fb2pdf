#!/bin/sh

PROJECT_HOME=`pwd`

if [ -z `which hadoop` ]; then
	echo "Hadoop is not found. Please install Hadoop and add it to your PATH environment variable";
	exit 1;
fi

ant clean jar

hadoop fs -rmr dist/
hadoop fs -rmr stopwords/
hadoop fs -rmr data/
hadoop fs -rmr out/
hadoop fs -rmr build/
hadoop fs -rmr lib/

hadoop fs -mkdir data/
hadoop fs -mkdir stopwords/
hadoop fs -mkdir lib/

hadoop fs -put dist/fb2pdf.job dist/fb2pdf.job
hadoop fs -put etc/stopwords/* stopwords/
hadoop fs -put lib/cluster/mahout-0.3/*.jar lib/
hadoop fs -put lib/*.jar lib/
hadoop fs -put test_data/hamake/*.fb2 data/

hadoop jar hamake/lib/hamake-2.0b-2.jar -f file://$PROJECT_HOME/hamake/clusterizer-dfs.xml -j 1
