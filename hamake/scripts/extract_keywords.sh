#!/bin/sh

LOC=`pwd`

export HADOOP_CLASSPATH=$LOC/build/:$LOC/lib/commons-logging-1.0.4.jar:$LOC/lib/commons-lang-2.4.jar

rm -rf /Users/lord/tmp/dfs/fb2pdf/keywords/Nabokov_Vladimir__Drugie_berega.fb2.csv

~/java/hadoop-0.18.3/bin/hadoop com.fb2pdf.hadoop.FB2KeywordsExtractor \
	/Users/lord/tmp/dfs/fb2pdf/books/Nabokov_Vladimir__Drugie_berega.fb2 \
	/Users/lord/tmp/dfs/fb2pdf/keywords/Nabokov_Vladimir__Drugie_berega.fb2.csv