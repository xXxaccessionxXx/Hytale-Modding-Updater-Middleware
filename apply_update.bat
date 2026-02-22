@echo off
setlocal

:: The PID of the Java Agent process is passed as the first argument
set JAVA_PID=%1

if "%JAVA_PID%"=="" (
    echo Error: Missing PID.
    exit /b 1
)

echo Waiting for Java process %JAVA_PID% to exit...

:WAIT_LOOP
tasklist /FI "PID eq %JAVA_PID%" | grep "%JAVA_PID%" >nul
if %ERRORLEVEL% equ 0 (
    :: Process is still running, wait 1 second and check again
    timeout /t 1 /nobreak >nul
    goto WAIT_LOOP
)

echo Process %JAVA_PID% exited. Proceeding with update...

:: Replace the old agent with the new one
if exist "middleware-agent-new.jar" (
    :: Since the file might be locked briefly, loop a few times
    set RETRIES=5
    :REPLACE_LOOP
    copy /Y "middleware-agent-new.jar" "middleware-agent.jar" >nul
    if %ERRORLEVEL% equ 0 (
        echo Update successful! New middleware installed.
        del "middleware-agent-new.jar"
        exit /b 0
    ) else (
        set /A RETRIES-=1
        if !RETRIES! gtr 0 (
            timeout /t 1 /nobreak >nul
            goto REPLACE_LOOP
        ) else (
            echo Error: Could not overwrite middleware-agent.jar.
            exit /b 1
        )
    )
) else (
    echo update-agent-new.jar not found, nothing to do.
)

endlocal
exit /b 0
