This file describes installation and configuration of Fb2PDF Hamake job that
finds similar books in fb2 format.

1) download Elastic Map Reduce tools from 
http://developer.amazonwebservices.com/connect/entry.jspa?categoryID=266&externalID=2264
unpack it to the directory of your choise

2) download s3cmd from http://s3tools.org/s3cmd and unpack it to the directory of your choise.
Then add the folder where you've unpacked s3cmd to the PATH environment variable

3) edit file clusterizer-s3.xml and set 'fb2pdf.hamake.bucket' to the name you the bucket,
where jar files of Hamake, Mahout, configuration data and result will be located.
Also set 'book.storage.bucket' to the name of backet where books in fb2 are located. 

4) open configuration.json file and set following configuration parameters:

aws_similar_books_url - http://s3.amazonaws.com/<fb2pdf.hamake.bucket>/similar_books/clusters
elastic_mapreduce_command - path to the directory where you've unpacked  Elastic Map Reduce tools
hamake_jar - s3://<fb2pdf.hamake.bucket>/lib/hamake-2.0b-3.jar
hamake_file -s3://<fb2pdf.hamake.bucket>/clusterizer-s3.xml
access_id - AWS Access ID
private_key - AWS Private Key
keypair - name of your AWS key-pair
log_uri - s3://<fb2pdf.hamake.bucket>/<folder_with_job_logs>
mysql_host - mysql host name
mysql_user - mysql user name
mysql_pass - mysql password
mysql_db - name of mysql database

5) configure s3cmd by running s3cmd --configure

6) copy lib, stopwords, clusterizer-s3.xml, configuration.json to s3://<fb2pdf.hamake.bucket>/

s3cmd --recursive put lib s3://<fb2pdf.hamake.bucket>/
s3cmd --recursive put stopwords s3://<fb2pdf.hamake.bucket>/
s3cmd put clusterizer-s3.xml s3://<fb2pdf.hamake.bucket>/
s3cmd put configuration.json s3://<fb2pdf.hamake.bucket>/  

7) set public access to s3://<fb2pdf.hamake.bucket>/similar_books/

s3cmd setacl --recursive --acl-public s3://<fb2pdf.hamake.bucket>/similar_books/

7) chmod 755 LoadSimilarBooks.py   

8) add column book_group to OriginalBooks table:

ALTER TABLE OriginalBooks ADD book_group INT;

9) start LoadSimilarBooks.py:

./LoadSimilarBooks.py -r

10) Finally you can set cron job that will start LoadSimilarBooks.py every week:

run 'crontab -e' and add line
0 0 * * 0 <path_to_fb2pdf-hamake>/LoadSimilarBooks.py -r -f <path_to_fb2pdf-hamake>/fb2pdf-hamake.log -l debug


   