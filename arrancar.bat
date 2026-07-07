@echo off
rem ============================================================
rem  Arranca TODO el sistema con un doble clic:
rem   - ms-pagos   (puerto 8084)
rem   - ms-reserva (puerto 8083, perfil local)
rem  Cada microservicio corre en su propia ventana/JVM.
rem  Consola de pruebas: http://localhost:8083/
rem ============================================================

echo Iniciando ms-pagos (8084)...
start "ms-pagos [8084]" cmd /k "cd /d %~dp0ms-pagos && mvnw.cmd spring-boot:run"

echo Iniciando ms-reserva (8083, perfil local)...
start "ms-reserva [8083]" cmd /k "cd /d %~dp0ms-reserva && set SPRING_PROFILES_ACTIVE=local&& mvnw.cmd spring-boot:run"

echo.
echo Cuando ambas ventanas muestren "Tomcat started", abre:
echo   http://localhost:8083/          (portal de reservas)
echo   http://localhost:8083/swagger-ui.html
echo   http://localhost:8084/api/pagos (pagos procesados)
echo.
echo Para detener: cierra las dos ventanas (o Ctrl+C en cada una).
pause
