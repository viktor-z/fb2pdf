@echo off

for /f tokens^=*^ delims^=^ eol^= %%i in ('cmd /c %~d0\%~p0\findjava.cmd') do set JAVA_DIR=%%i

echo Start time: %time%
echo -----------------------
"%JAVA_DIR%\java" -Xmx512m -jar "%~d0\%~p0\lib\fb2pdf.jar" %*
echo ---------------------
echo End time: %time%


