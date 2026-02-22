@echo off
echo ===================================================
echo Hytale Updater Middleware - Global Installation
echo ===================================================
echo.
echo Building Middleware Agent...
powershell -Command "if (!(Test-Path 'lib')) { New-Item -ItemType Directory -Force -Path 'lib' }; if (!(Test-Path 'lib\asm-9.7.1.jar')) { Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/ow2/asm/asm/9.7.1/asm-9.7.1.jar' -OutFile 'lib\asm-9.7.1.jar' }"

rmdir /s /q agent_build 2>nul
mkdir agent_build
set "JAVA_TOOL_OPTIONS="
set "_JAVA_OPTIONS="
set "JDK_JAVA_OPTIONS="

javac -cp ".;lib\asm-9.7.1.jar" -d agent_build *.java

echo Extracting ASM library and packaging...
powershell -Command "Add-Type -AssemblyName System.IO.Compression.FileSystem; [System.IO.Compression.ZipFile]::ExtractToDirectory('lib\asm-9.7.1.jar', 'agent_tmp');"

xcopy agent_tmp\* agent_build\ /s /e /y /q >nul
rmdir /s /q agent_tmp

rmdir /s /q agent_build\META-INF 2>nul
mkdir agent_build\META-INF
copy MANIFEST.MF agent_build\META-INF\ >nul

del middleware-agent.jar 2>nul
powershell -Command "Add-Type -AssemblyName System.IO.Compression.FileSystem; $zip = [System.IO.Compression.ZipFile]::Open('middleware-agent.jar', 'Create'); [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, 'agent_build\META-INF\MANIFEST.MF', 'META-INF/MANIFEST.MF') | Out-Null; foreach($f in Get-ChildItem 'agent_build' -Recurse -File) { $rel = $f.FullName.Substring((Get-Item 'agent_build').FullName.Length + 1).Replace('\', '/'); if ($rel -ne 'META-INF/MANIFEST.MF') { [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $f.FullName, $rel) | Out-Null } }; $zip.Dispose()"

rmdir /s /q agent_build

echo.
echo ===================================================
echo Setting up Global Java Hook...
echo Any Java application started by your user will now
echo load this Agent, but it will go dormant unless it
echo detects the Hytale Server!
echo ===================================================

:: Set the environment variable for the current user WITHOUT quotes by using DOS short path 8.3
setx JAVA_TOOL_OPTIONS -javaagent:%~sdp0middleware-agent.jar
setx _JAVA_OPTIONS -javaagent:%~sdp0middleware-agent.jar

echo.
echo Setup Complete! 
echo Please close and reopen your launcher (e.g., CurseForge) or terminal
echo for the environment variables to take effect.
echo.
pause
