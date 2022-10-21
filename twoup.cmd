@echo off

for /f tokens^=*^ delims^=^ eol^= %%i in ('cmd /c %~d0\%~p0\findjava.cmd') do set JAVA_DIR=%%i

if "%JAVA_DIR%" == "" (
    exit 1
)
if not exist "%JAVA_DIR%" (
    exit 1
)

"%JAVA_DIR%\java" -cp "%~d0\%~p0\lib\fb2pdf.jar" org.trivee.utils.TwoUp %*
