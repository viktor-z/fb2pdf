@echo off

for /f "tokens=4" %%i in ('chcp') do set CONSOLECP=cp%%i
for /f tokens^=*^ delims^=^ eol^= %%i in ('cmd /c "%~d0\%~p0\findjava.cmd"') do set JAVA_DIR=%%i

if "%JAVA_DIR%" == "" (
    exit 1
)
if not exist "%JAVA_DIR%" (
    exit 1
)
@echo -----------------------
@echo Using Java from "%JAVA_DIR%"

set _argcActual=0
for %%i in (%*) do set /A _argcActual+=1

@echo -----------------------
@echo Start time: %time%
@echo -----------------------

if "%_argcActual%" EQU "0" GOTO NOARGS

call :GETTEMPNAME
FOR %%I in (%*) do cmd /u /s /c echo %%~I >>"%TMPFILE%"
"%JAVA_DIR%\java.exe" -Xmx512m -jar "%~d0%~p0lib\fb2pdf.jar" -e UTF-8 -c %CONSOLECP% @"%TMPFILE%"
del /Q /F "%TMPFILE%" >nul
goto CONT

:NOARGS
"%JAVA_DIR%\java.exe" -Xmx512m -jar "%~d0%~p0lib\fb2pdf.jar"

:CONT

@echo ---------------------
@echo End time: %time%
@echo ---------------------

goto FIN

:GETTEMPNAME
set TMPFILE=%TMP%\fb2pdf-%RANDOM%-%TIME:~6,5%.tmp
if exist "%TMPFILE%" GOTO :GETTEMPNAME

:FIN
