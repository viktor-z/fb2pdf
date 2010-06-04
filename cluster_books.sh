#!/bin/sh
PROJECT_HOME=`dirname $0`
PROJECT_HOME=`cd "$PROJECT_HOME"; pwd`

if [ -z `which hadoop` ]; then
	echo "Hadoop is not found. Please install Hadoop and add it to your PATH environment variable";
	exit 1;
fi

if [ ! -f $PROJECT_HOME/dist/fb2pdf.job ]; then
	echo "dist/fb2pdf.job file is not found. Please run ant clean jar first"
	exit 1;
fi

if [ -z $BOOKS_TO_CLUSTER ]; then 
	echo "Please set environment variable BOOKS_TO_CLUSTER - path to a directory with books in fb2 format"
	exit 1;
fi

if [ -z $CATEGORIZED_BOOKS ]; then
        echo "Please set environment variable CATEGORIZED_BOOKS - path to a directory where you would like to output categorized of books"
        exit 1;
fi

IS_LOCAL=""
if [ x$1 = xlocal ]; then
	echo "Running in local mode"
	IS_LOCAL="-j 1"
else
	echo "Copying resources to HDFS..."
	hadoop fs -rmr $BOOKS_TO_CLUSTER
	hadoop fs -put $BOOKS_TO_CLUSTER $BOOKS_TO_CLUSTER
	hadoop fs -rmr dist
	hadoop fs -put $PROJECT_HOME/dist/fb2pdf.job dist/fb2pdf.job
	hadoop fs -rmr lib/cluster/mahout-0.3/
	hadoop fs -mkdir lib/cluster/mahout-0.3/
	hadoop fs -copyFromLocal $PROJECT_HOME/lib/cluster/mahout-0.3/mahout-utils-0.3.jar lib/cluster/mahout-0.3/
fi

hadoop jar $PROJECT_HOME/hamake/lib/hamake-2.0b-2.jar -f file://$PROJECT_HOME/hamake/clusterizer.xml $IS_LOCAL
