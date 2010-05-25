#!/bin/sh
PROJECT_HOME=`dirname $0`

if [ -z `which hadoop` ]; then
	echo "Hadoop is not found. Please install Hadoop and add it to your PATH environment variable";
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
fi

hadoop jar $PROJECT_HOME/hamake/lib/hamake-2.0b-2.jar -f $PROJECT_HOME/hamake/clusterizer.xml $IS_LOCAL
