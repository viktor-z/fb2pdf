@echo off

for /f tokens^=*^ delims^=^ eol^= %%i in ('cmd /c %~d0\%~p0\findjava.cmd') do set JAVA_DIR=%%i

if "%JAVA_DIR%" == "" (
    exit 1
)
if not exist "%JAVA_DIR%" (
    exit 1
)

SET PROLOG=declare default element namespace 'http://www.gribuser.ru/xml/fictionbook/2.0'; declare namespace l = 'http://www.w3.org/1999/xlink';

"%JAVA_DIR%\java" -cp "%~d0\%~p0\lib\fb2pdf.jar" nux.xom.tests.XQueryCommand --query="{%PROLOG% %~2}" --update="{%PROLOG% %~3}" %1 --out %1.transformed
