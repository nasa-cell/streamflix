@echo off
chcp 65001 >nul
title StreamFlix
cd /d "%~dp0"

echo.
echo  ===============================
echo       S T R E A M F L I X
echo  ===============================
echo.

where java >nul 2>nul
if errorlevel 1 (
  echo  [ERROR] No se encontro Java. Instala Java 17 o mas nuevo.
  pause
  exit /b 1
)

sc query MySQL84 | find "RUNNING" >nul
if errorlevel 1 (
  echo  MySQL esta apagado. Intentando encenderlo...
  net start MySQL84 >nul 2>nul
  if errorlevel 1 (
    echo  [AVISO] No se pudo encender MySQL84. Abre este archivo como administrador
    echo          o enciende el servicio MySQL84 desde "Servicios" de Windows.
  )
)

echo  Compilando el codigo Java...
set "COMPILADO=%TEMP%\streamflix-compilado"
if exist "%COMPILADO%" rmdir /s /q "%COMPILADO%"
javac -encoding UTF-8 -d "%COMPILADO%" -cp "1-codigo\lib\mysql-connector-j-8.4.0.jar" -sourcepath 1-codigo\java 1-codigo\java\streamflix\Principal.java 1-codigo\java\streamflix\PruebasRendimiento.java 1-codigo\java\streamflix\PruebasDatosReales.java
if errorlevel 1 (
  echo  [ERROR] El codigo no compilo. Revisa los mensajes de arriba.
  pause
  exit /b 1
)

echo  Abriendo http://localhost:8095 ...
start "" /b cmd /c "timeout /t 2 /nobreak >nul & start "" http://localhost:8095"
java -Dstdout.encoding=UTF-8 -cp "%COMPILADO%;1-codigo\lib\mysql-connector-j-8.4.0.jar" streamflix.Principal
pause
