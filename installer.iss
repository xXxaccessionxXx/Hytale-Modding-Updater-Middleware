[Setup]
AppName=Hytale Modding Updater Middleware
AppVersion=1.0.0
DefaultDirName={localappdata}\HytaleMiddlewareAgent
DefaultGroupName=Hytale Middleware
UninstallDisplayIcon={app}\assets\logo.ico
Compression=lzma2
SolidCompression=yes
OutputDir=.
OutputBaseFilename=Hytale_Middleware_Setup_1.0.0
SetupIconFile=assets\logo.ico
PrivilegesRequired=lowest
WizardStyle=modern
WizardImageFile=assets\wizard_sidebar.bmp
WizardSmallImageFile=assets\wizard_top.bmp
DisableWelcomePage=no
DisableDirPage=yes
DisableProgramGroupPage=yes

[Files]
Source: "middleware-agent.jar"; DestDir: "{app}"; Flags: ignoreversion
Source: "install_global_hook.bat"; DestDir: "{app}"; Flags: ignoreversion
Source: "assets\logo.ico"; DestDir: "{app}\assets"; Flags: ignoreversion

[Run]
Filename: "{app}\install_global_hook.bat"; Description: "Install Global Java Hook (Required)"; Flags: postinstall runhidden nowait

[UninstallRun]
; When uninstalling, we should ideally invoke a script to remove the global hook. 
; For now, if the user uninstalls, the environment variable might remain, but the jar is deleted so it safely fails/ignores.
; We can add an uninstall_global_hook.bat later if needed.
