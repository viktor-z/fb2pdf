@echo off

setlocal ENABLEEXTENSIONS

REM Check for local Java directory first

if exist "%~dp0jdk" (
    set HomeValue=%~dp0jdk
    goto PROCESS
)
if exist "%~dp0jre" (
    set HomeValue=%~dp0jre
    goto PROCESS
)

REM If recent JDk was installed - expect JAVA_HOME

if exist "%JAVA_HOME%" (
    set HomeValue=%JAVA_HOME%
    goto PROCESS
)

REM Check for global Java installation

set KEY_JRE=HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Runtime Environment
set JRE_VER=CurrentVersion
set JRE_HOME=JavaHome

if %PROCESSOR_ARCHITECTURE% == x86 (
    if not defined PROCESSOR_ARCHITEW6432 (
        REM Windows 32 bits
        goto DONATIVE
    )
) else (
    REM Running 64 bits process in Windows 64 bits 
    goto DONATIVE
)
@echo Running 32 bits process in Windows 64 bits 

FOR /F "usebackq skip=2 tokens=1,2,*" %%A IN (`REG QUERY "%KEY_JRE%" /v %JRE_VER% /reg:64 2^>nul`) DO (
    set VerName=%%A
    set VerType=%%B
    set VerValue=%%C
)

if not defined VerName (
    REM 64 bit %KEY_JRE%\%JRE_VER% not found.
    goto DONATIVE
)

FOR /F "usebackq skip=2 tokens=1,2,*" %%A IN (`REG QUERY "%KEY_JRE%\%VerValue%" /v %JRE_HOME% /reg:64 2^>nul`) DO (
    set HomeName=%%A
    set HomeType=%%B
    set HomeValue=%%C
)

if not defined HomeName (
    REM 64 bit %KEY_JRE%\%JRE_HOME% not found.
    goto DONATIVE
)

goto PROCESS

:DONATIVE

FOR /F "usebackq skip=2 tokens=1,2,*" %%A IN (`REG QUERY "%KEY_JRE%" /v %JRE_VER% 2^>nul`) DO (
    set VerName=%%A
    set VerType=%%B
    set VerValue=%%C
)

if not defined VerName (
    @echo Unable to locate Java installation: "%KEY_JRE%\%JRE_VER%" not found, "JAVA_HOME" environment is not set properly and there is no JDK or JRE directories in the script start directory. 1>&2
    exit 1
)

FOR /F "usebackq skip=2 tokens=1,2,*" %%A IN (`REG QUERY "%KEY_JRE%\%VerValue%" /v %JRE_HOME% 2^>nul`) DO (
    set HomeName=%%A
    set HomeType=%%B
    set HomeValue=%%C
)

if not defined HomeName (
    @echo Unable to locate Java installation: "%KEY_JRE%\%JRE_HOME%" not found, "JAVA_HOME" environment is not set properly and there is no JDK or JRE directories in the script start directory. 1>&2
    exit 1
)

:PROCESS

echo %HomeValue%\bin
