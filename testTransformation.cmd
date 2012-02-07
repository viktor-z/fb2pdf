REM remove square brackets from note link
SET QUERY=//a[@type='note']/text()
set MORPHER=replace(., '[\[\]]', '')

call transform.cmd %1 "%QUERY%" "%MORPHER%"
