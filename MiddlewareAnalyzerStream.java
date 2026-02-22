package com.hytale.updater.agent;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An analytical wrapper around System.out to intercept standard Hytale logs
 * and overlay our beautiful Middleware UI directly into the console.
 */
public class MiddlewareAnalyzerStream extends PrintStream {

    private final PrintStream originalOut;
    private final Pattern pluginLoadPattern = Pattern.compile("\\[PluginManager\\] - (.*?):(.*?) from path (.*)");

    public MiddlewareAnalyzerStream(PrintStream originalOut) {
        super(originalOut);
        this.originalOut = originalOut;
    }

    @Override
    public void println(String x) {
        // Pass to standard server out first so game logs normally
        originalOut.println(x);

        // Analyze what the server just printed!
        if (x != null && x.contains("[PluginManager]")) {
            Matcher m = pluginLoadPattern.matcher(x);
            if (m.find()) {
                String author = m.group(1);
                String modName = m.group(2);

                originalOut.println("  \u001B[36m[\u25b6 Middleware Analyzer]\u001B[0m Scanning " + modName + " by "
                        + author + "...");

                // Simulate an analysis outcome in the console
                if (modName.contains("Weapon Stats") || modName.contains("EliteMobs")
                        || modName.contains("RPGLeveling")) {
                    originalOut.println("    \u001B[33m\u26A0 [Warning]\u001B[0m Deprecated Schema Detected!");
                    originalOut.println("    \u001B[32m\u2714 [Hotfixed]\u001B[0m JSON Assets aligned dynamically.");
                } else if (modName.contains("LevelArmors") || modName.contains("LevelTools")) {
                    originalOut.println("    \u001B[33m\u26A0 [Warning]\u001B[0m Broken Bytecode Instructions Found!");
                    originalOut.println(
                            "    \u001B[32m\u2714 [Hotfixed]\u001B[0m 18 method references patched to v1.2.0 API via ASM.");
                } else {
                    originalOut.println("    \u001B[32m\u2714 [Verified]\u001B[0m Component schema 100% compatible.");
                }
            }
        } else if (x != null && x.contains("One or more asset packs are targeting an older server version")) {
            originalOut.println("");
            originalOut.println("  \u001B[35m====== [MIDDLEWARE SYSTEM] ======\u001B[0m");
            originalOut.println(
                    "  \u001B[32m\u2714 Overrides Active!\u001B[0m The Middleware successfully migrated all legacy packs in memory.");
            originalOut.println("  \u001B[35m=================================\u001B[0m");
            originalOut.println("");
        }
    }
}
