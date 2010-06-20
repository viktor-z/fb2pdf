#! /usr/bin/env python

import MySQLdb
import getopt, sys, os, re, time
import json
import urllib

def loadClustrersFromURL(url):
     sock = urllib.urlopen(url)
     clusters = []
     try:
         while 1:
             line = sock.readline()
             if not line:
                 break
             values = line.split();
             clusters.append(values[1:])     
     finally:
         sock.close()
     return clusters;
 
def storeBookGroups(mysqlHost, mysqlUser, mysqlPassword, mysqlDB, clusters):       
    
    bookGroups = []
    conn = MySQLdb.connect (host = mysqlHost or "localhost", user = mysqlUser, passwd = mysqlPassword, db = mysqlDB or "FB2PDF")
    try:   
        cursor = conn.cursor ()
        try:
            for cluster in clusters:
                bookGroup = []
                for bookHash in cluster:                
                    cursor.execute ('''select id from OriginalBooks where storage_key = '%s' ''' % bookHash.split(".")[0])
                    row = cursor.fetchone ()
                    if row is not None:
                        bookGroup.append(row[0])
                bookGroups.append(bookGroup)
            insertGroups = []
            for bookGroup in bookGroups:
                for i in range(len(bookGroup)):
                    for j in range(i, len(bookGroup)):
                        if(i != j):
                            insertGroups.append([str(bookGroup[i]), str(bookGroup[j])])
            if(insertGroups.__len__() > 0):
                cursor.executemany('''delete from SimilarBooks where (book_id=%s and similar_book_id=%s) ''', insertGroups)
                cursor.executemany('''delete from SimilarBooks where (similar_book_id=%s and book_id=%s) ''', insertGroups)
                cursor.executemany('''insert into SimilarBooks (book_id, similar_book_id)
                      values (%s, %s)''', insertGroups)                            
        finally:
            cursor.close ()
    finally:
        conn.close ()
    return bookGroups
    
def loadConfiguration(configurationFile):
    fsock = open(configurationFile, "r")
    try:        
        return json.loads(fsock.read())
    finally:
         fsock.close()   
         
def startAndWaitEMRJob(elasticMapreduceCommandPath, hamakeJarPath, hamakeFilePath, awsKeyPair):
    if not os.path.isfile(elasticMapreduceCommandPath):
        print ''' can't find %s ''' % elasticMapreduceCommandPath
        return
    command = ''' %s --create --JAR %s --wait-for-step --main-class com.codeminders.hamake.Main --args -f,%s,-j,10 --key-pair %s --name FB2PDF_CLUSTERER --step-name FB2PDF_CLUSTERER''' % \
            (elasticMapreduceCommandPath,\
            hamakeJarPath,\
            hamakeFilePath,\
            awsKeyPair)
    print "Starting EMR job " + command
    result = os.popen(command).read()
    jobId = re.search(r'\bj-(\w)+\b', result).group(0)
    if not jobId == None:
        status = None
        print "Successfully started job " + jobId
        attempts = 0
        while 1:            
            result = os.popen(''' %s --list -j %s ''' % (elasticMapreduceCommandPath, jobId)).read()
            if str(result).strip().split() > 2 and str(result).strip().split()[-1] == "COMPLETED":
                attempts = 0
                if str(result).strip().split()[-1] != "PENDING" and result.split()[-1] != "RUNNING":
                    status = str(result).strip().split()[-1]
                    break
                else:
                    time.sleep(10)
            else:
                if attempts > 60:
                    print "Contact with AWS lost for more than 10 minutes"
                    return 0
                attempts += 1
                time.sleep(10);
        print "Job finished with status " + status
        if status == 'COMPLETED':
            return 1
        else:
            return 0
    else:
        print "Failed to start job " + result
        return 0
 
def main(configurationFile, clustersFile):     
    
    configuration = loadConfiguration(configurationFile or "configuration.json")    
    
    #clusters = loadClustrersFromURL(configuration["aws_similar_books_url"] or "http://s3.amazonaws.com/fb2pdf-hadoop/clusters/part-00000")
    #storeBookGroups(configuration["mysql_host"], configuration["mysql_user"], configuration["mysql_pass"], configuration["mysql_db"],  clusters)    
    if startAndWaitEMRJob(configuration["elastic_mapreduce_command"], configuration["hamake_jar"], configuration["hamake_file"], configuration["aws_key"]):
        print "Loading similar books in database"
    
    sys.exit(0);
        
try:
    opts, args = getopt.getopt(sys.argv[1:], "hc:f:", ["help", "configuration=", "file="])
except getopt.GetoptError, err:
    print str(err)
    sys.exit(1)
configurationFile = None;
clustersFile = None;
for o, a in opts:
    if o in ("-h", "--help"):
        usage()
        sys.exit(0);
    elif o in ("-c", "--configuration"):
        configurationFile = a
    elif o in ("-f", "--file"):
        clustersFile = a
main(configurationFile, clustersFile)

#select b.title, b.storage_key from SimilarBooks sb, OriginalBooks b where sb.book_id = '72350' and sb.similar_book_id=b.id union select b.title, b.storage_key from SimilarBooks sb, OriginalBooks b where sb.similar_book_id = '72350' and sb.book_id=b.id;
