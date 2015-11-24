@echo off

setlocal ENABLEEXTENSIONS

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
    @echo %KEY_JRE%\%JRE_VER% not found.
    exit 1
)

FOR /F "usebackq skip=2 tokens=1,2,*" %%A IN (`REG QUERY "%KEY_JRE%\%VerValue%" /v %JRE_HOME% 2^>nul`) DO (
    set HomeName=%%A
    set HomeType=%%B
    set HomeValue=%%C
)

if not defined HomeName (
    @echo %KEY_JRE%\%JRE_HOME% not found.
	exit 1
)

:PROCESS

echo %HomeValue%\bin
