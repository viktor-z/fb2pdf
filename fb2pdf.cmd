@echo Start time: %time%
@echo -----------------------
@java -Xmx512m -jar "%~d0\%~p0\lib\fb2pdf.jar" %*
@echo ---------------------
@echo End time: %time%


