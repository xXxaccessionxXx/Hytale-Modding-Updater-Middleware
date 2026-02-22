using System;
using System.Diagnostics;
using System.Linq;

namespace JavaProxy
{
    class Program
    {
        static void Main(string[] args)
        {
            Console.WriteLine("[Hytale Middleware Proxy] Intercepting Java launch...");

            string originalJava = System.IO.Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "java_original.exe");
            string agentPath = @"c:\Users\kasey\Hytale Modding Updater Middleware\middleware-agent.jar";
            
            // Build the arguments, inserting our agent
            string newArgs = $"-javaagent:\"{agentPath}\" " + string.Join(" ", args);

            ProcessStartInfo startInfo = new ProcessStartInfo
            {
                FileName = originalJava,
                UseShellExecute = false,
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                CreateNoWindow = true
            };
            
            startInfo.EnvironmentVariables.Remove("JDK_JAVA_OPTIONS");
            startInfo.EnvironmentVariables.Remove("JAVA_TOOL_OPTIONS");
            startInfo.EnvironmentVariables.Remove("_JAVA_OPTIONS");
            
            startInfo.ArgumentList.Add($"-javaagent:{agentPath}");
            foreach (var arg in args)
            {
                startInfo.ArgumentList.Add(arg);
            }

            Console.WriteLine($"[Hytale Middleware Proxy] Launching: {originalJava} with ArgumentList");

            using (Process process = new Process { StartInfo = startInfo })
            {
                process.OutputDataReceived += (sender, e) => { if (e.Data != null) Console.WriteLine(e.Data); };
                process.ErrorDataReceived += (sender, e) => { if (e.Data != null) Console.Error.WriteLine(e.Data); };

                process.Start();
                process.BeginOutputReadLine();
                process.BeginErrorReadLine();
                process.WaitForExit();

                Environment.Exit(process.ExitCode);
            }
        }
    }
}
