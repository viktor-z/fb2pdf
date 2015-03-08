@echo off
start javaw -cp ./UI;./lib/pivot-core-2.0.1.jar;./lib/pivot-wtk-2.0.1.jar;./lib/pivot-wtk-terra-2.0.1.jar;./lib/js.jar;./lib/js-engine.jar;./lib/fb2pdf.jar org.apache.pivot.wtk.ScriptApplication --src="/main.bxml" --resources="localization"
rem start javaw -Duser.language=ru -cp ./UI;./lib/pivot-core-2.0.1.jar;./lib/pivot-wtk-2.0.1.jar;./lib/pivot-wtk-terra-2.0.1.jar;./lib/js.jar;./lib/js-engine.jar;./lib/fb2pdf.jar org.apache.pivot.wtk.ScriptApplication --src="/main.bxml" --resources="localization"
