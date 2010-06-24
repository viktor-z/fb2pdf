#! /usr/bin/env python

import MySQLdb
import getopt, sys, os, re, time, logging
import json
import urllib

def loadClustrersFromURL(url, logger):
    attempts = 0
    while 1:
        try:
            sock = urllib.urlopen(url)
            try:
                clusters = []
                while 1:
                    line = sock.readline()
                    if not line:
                        return []
                    values = re.findall(r'\b\w{32}.fb2\b', line)
                    if len(values) > 0:
                        clusters.append(values[1:])
            finally:
                sock.close() 
                return clusters
        except IOError:
            if attempts > 6:
                logger.error("Could not download %s" % url)
                break
            attempts += 1
            time.sleep(10 * attempts)
            continue
    return [];
 
def configureLogger(filename = None, levelName = "info"):
    LEVELS = {'debug': logging.DEBUG,
          'info': logging.INFO,
          'warning': logging.WARNING,
          'error': logging.ERROR,
          'critical': logging.CRITICAL}

    level = LEVELS.get(levelName, logging.NOTSET)
    if filename is None:
        logging.basicConfig(level=level, format="%(asctime)s %(levelname)s - %(message)s")
    else:
        logging.basicConfig(filename = filename, level=level, format="%(asctime)s %(levelname)s - %(message)s")
    
    return logging.getLogger("LoadSimilarBooks")
     
def storeBookGroups(mysqlHost, mysqlUser, mysqlPassword, mysqlDB, clusters, logger):       
    
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
                if len(bookGroup) > 1:
                    for i in range(len(bookGroup)):
                        insertGroups.append([str(bookGroup[0]), str(bookGroup[i])])
            if(insertGroups.__len__() > 0):
                cursor.executemany('''update OriginalBooks set book_group = %s where id = %s ''', insertGroups)               
                logger.info("Loaded %i raws" % len(insertGroups))             
        finally:
            cursor.close ()
    finally:
        conn.commit()
        conn.close ()
    return bookGroups
    
def loadConfiguration(configurationFile):
    fsock = open(configurationFile, "r")
    try:        
        return json.loads(fsock.read())
    finally:
         fsock.close()   

def startAndWaitEMRJob(elasticMapreduceCommandPath, hamakeJarPath, hamakeFilePath, accessId, privateKey, keyPair, logUri, logger, instances):
    if not os.path.isfile(elasticMapreduceCommandPath):
        logger.error(''' can't find %s ''' % elasticMapreduceCommandPath)
        return
    command = ''' %s --create --JAR %s --wait-for-step --main-class com.codeminders.hamake.Main --args -f,%s,-j,%i --key-pair %s --name FB2PDF_CLUSTERER --step-name FB2PDF_CLUSTERER -a %s -k %s --log-uri %s --num-instances %i''' % \
            (elasticMapreduceCommandPath,\
            hamakeJarPath,\
            hamakeFilePath,\
            instances, \
            keyPair, \
            accessId, \
            privateKey, \
            logUri, \
            instances)
    logger.debug("Starting EMR job " + command)
    result = os.popen(command).read()
    jobId = re.search(r'\bj-(\w)+\b', result).group(0)
    if not jobId == None:
        status = None
        logger.info("Successfully started job " + jobId + ". Waiting for its completion...")
        attempts = 0
        while 1:            
            result = os.popen(''' %s --list -j %s ''' % (elasticMapreduceCommandPath, jobId)).read()
            if str(result).strip().split() > 1:
                status = str(result).strip().split()[-2]
                if status in ("COMPLETED", "RUNNING", "PENDING", "TERMINATED", "FAILED", "SHUTTING_DOWN", "WAITING", "CANCELLED"):
                    attempts = 0
                    if status not in ("PENDING", "RUNNING", "WAITING", "SHUTTING_DOWN"):
                        break
                    else:
                        time.sleep(60)
                        continue
            if attempts > 60:
                logger.error("Contact with AWS lost for more than 10 minutes")
                return 0
            attempts += 1
            time.sleep(10);
        logger.info("Job finished with status " + status)
        if status == 'COMPLETED':
            return 1
        else:
            return 0
    else:
        logger.error("Failed to start job " + result)
        return 0
 
def main(configurationFile, logFile, logLevel, runJob):     
    
    configuration = loadConfiguration(configurationFile or "configuration.json")
    logger = configureLogger(logFile, logLevel)
    
    
    if runJob:
        if not startAndWaitEMRJob(configuration["elastic_mapreduce_command"], \
                                  configuration["hamake_jar"], \
                                  configuration["hamake_file"], \
                                  configuration["access_id"], \
                                  configuration["private_key"], \
                                  configuration["keypair"], \
                                  configuration["log_uri"], \
                                  logger,
                                  2):
            exit(1)
    logger.info("Loading similar books in database")
    clusters = loadClustrersFromURL(configuration["aws_similar_books_url"] or "http://s3.amazonaws.com/fb2pdf-hamake/clusters/part-00000", logger)
    if len(clusters) > 0:
        storeBookGroups(configuration["mysql_host"], configuration["mysql_user"], configuration["mysql_pass"], configuration["mysql_db"],  clusters, logger)
    else:
        logger.warning("No similar books found")
    sys.exit(0);
        
try:
    opts, args = getopt.getopt(sys.argv[1:], "hc:f:l:ri:", ["help", "configuration=", "log_file=", "log_level=", "run_job", "instances="])
except getopt.GetoptError, err:
    print str(err)
    sys.exit(1)
configurationFile = None;
logFile = None
logLevel = 'info'
runJob = 0
instances = 2;
for o, a in opts:
    if o in ("-h", "--help"):
        usage()
        sys.exit(0);
    elif o in ("-c", "--configuration"):
        configurationFile = a
    elif o in ("-f", "--log_file"):
        logFile = a
    elif o in ("-l", "--log_level"):
        if a in ('debug', 'info', 'warning', 'error', 'critical'):
            logLevel = a
        else:
            print "log level should be one of 'debug', 'info', 'warning', 'error' or 'critical'"
    elif o in ("-r", "--run_job"):
        runJob = 1
    elif o in ("-i", "--instances"):
        instances = a
main(configurationFile, logFile, logLevel, runJob)

#select b.title, b.storage_key from SimilarBooks sb, OriginalBooks b where sb.book_id = '72350' and sb.similar_book_id=b.id union select b.title, b.storage_key from SimilarBooks sb, OriginalBooks b where sb.similar_book_id = '72350' and sb.book_id=b.id;
