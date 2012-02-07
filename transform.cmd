SET CLASSPATH=%~d0\%~p0\lib\nux.jar
SET CLASSPATH=%CLASSPATH%;%~d0\%~p0\lib\xom.jar
SET CLASSPATH=%CLASSPATH%;%~d0\%~p0\lib\gnu-getopt.jar
SET CLASSPATH=%CLASSPATH%;%~d0\%~p0\lib\saxon8.jar

SET PROLOG=declare default element namespace 'http://www.gribuser.ru/xml/fictionbook/2.0'; declare namespace l = 'http://www.w3.org/1999/xlink';

java -cp "%CLASSPATH%" nux.xom.tests.XQueryCommand --query="{%PROLOG% %~2}" --update="{%PROLOG% %~3}" %1 --out %1.transformed

