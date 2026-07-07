@echo off
rem Detiene lo que este escuchando en los puertos 8083 y 8084.
for /f "tokens=5" %%p in ('netstat -ano ^| findstr :8083 ^| findstr LISTENING') do taskkill /PID %%p /F 2>nul
for /f "tokens=5" %%p in ('netstat -ano ^| findstr :8084 ^| findstr LISTENING') do taskkill /PID %%p /F 2>nul
echo Microservicios detenidos.
pause
