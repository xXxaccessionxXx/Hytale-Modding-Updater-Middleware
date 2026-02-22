@echo off
rmdir /s /q agent_build 2>nul
mkdir agent_build
set "JAVA_TOOL_OPTIONS="
set "_JAVA_OPTIONS="
set "JDK_JAVA_OPTIONS="

javac -cp ".;lib\asm-9.7.1.jar" -d agent_build *.java

powershell -Command "Add-Type -AssemblyName System.IO.Compression.FileSystem; [System.IO.Compression.ZipFile]::ExtractToDirectory('lib\asm-9.7.1.jar', 'agent_tmp');"
xcopy agent_tmp\* agent_build\ /s /e /y /q >nul
rmdir /s /q agent_tmp

rmdir /s /q agent_build\META-INF 2>nul
mkdir agent_build\META-INF
copy MANIFEST.MF agent_build\META-INF\ >nul

del middleware-agent.jar 2>nul
powershell -Command "Add-Type -AssemblyName System.IO.Compression.FileSystem; $zip = [System.IO.Compression.ZipFile]::Open('middleware-agent.jar', 'Create'); [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, 'agent_build\META-INF\MANIFEST.MF', 'META-INF/MANIFEST.MF') | Out-Null; foreach($f in Get-ChildItem 'agent_build' -Recurse -File) { $rel = $f.FullName.Substring((Get-Item 'agent_build').FullName.Length + 1).Replace('\', '/'); if ($rel -ne 'META-INF/MANIFEST.MF') { [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $f.FullName, $rel) | Out-Null } }; $zip.Dispose()"

rmdir /s /q agent_build
echo Build complete.
