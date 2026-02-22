package com.hytale.updater.agent;

import java.lang.instrument.Instrumentation;

/**
 * The entry point for the Middleware Watcher and Updater Java Agent.
 * This class hooks into the JVM *before* the Hytale Server's main method is
 * called,
 * granting us complete control to initialize our Watcher, analyze the diffs,
 * and
 * register our DynamicBytecodeTransformer with the JVM's classloading process.
 */
public class HytaleAgent {

    public static final String VERSION = "v1.0.0";

    /**
     * This method runs automatically when the JVM starts with the -javaagent flag.
     * It runs BEFORE public static void main() in the Hytale Server jar.
     *
     * @param agentArgs Any string arguments passed via the command line (e.g.,
     *                  -javaagent:middleware.jar=debug=true)
     * @param inst      The Java Instrumentation API, granting deep access to loaded
     *                  classes.
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        // --- GLOBAL HOOK DETECTION ---
        // Since this agent attaches to EVERY Java application launched by the user via
        // JAVA_TOOL_OPTIONS,
        // we must immediately go dormant if the process is not the Hytale Server or our
        // Simulation.
        String command = System.getProperty("sun.java.command", "");
        if (command == null)
            return;
        String lowerCommand = command.toLowerCase();
        if (!lowerCommand.contains("hytaleserver") && !lowerCommand.contains("hytale-server")
                && !lowerCommand.contains("modloadingsimulation")) {
            return; // Go dormant silently.
        }

        // 0. Install the Visual Boot Analyzer Hook!
        // This takes over System.out so we can read the server's stdout as it prints,
        // allowing us to inject our own beautiful UI analysis lines.
        System.setOut(new MiddlewareAnalyzerStream(System.out));

        System.out.println("======================================================");
        System.out.println("\u001B[35m[Hytale Middleware]\u001B[0m Java Agent Attached Successfully!");
        System.out.println("\u001B[35m[Hytale Middleware]\u001B[0m Detected Hytale Boot Sequence. Initializing...");

        // 1. Initialize the Watcher
        // Here we check for Hytale updates AND Middleware updates.
        AutoUpdater.checkForUpdates(VERSION);

        System.out.println("[Hytale Middleware] Checking for Server Updates...");
        boolean updateFound = false; // Mock

        if (updateFound) {
            System.out.println("[Hytale Middleware] Update found! Running Diff Engine...");
            // Run DiffEngine and generate TranslationDictionary
        } else {
            System.out.println("[Hytale Middleware] Server is up to date.");
        }

        // 2. Initialize the JSON Asset Migrator
        // Scan the mods/ folder and update JSON assets before the server even tries to
        // read them.
        System.out.println("[Hytale Middleware] Migrating outdated JSON Mod Assets...");
        String modsDir = "C:\\Users\\kasey\\AppData\\Roaming\\Hytale\\UserData\\Saves\\BIG BOI\\mods";
        int paddedTextures = TexturePadder.processModTextures(modsDir);

        // Display the UI Overlay to the user
        UINotificationOverlay.showNotification(paddedTextures);

        // 3. Register the Dynamic Bytecode Transformer
        // Instead of transforming '.class' files on disk and repacking them, a Java
        // Agent
        // allows us to intercept the bytes in-memory the exact moment the server tries
        // to load a mod class!
        System.out.println("[Hytale Middleware] Registering ClassFileTransformer...");
        inst.addTransformer(new ModClassInterceptor(), true);

        System.out.println("[Hytale Middleware] Pre-Boot Complete. Yielding control to Hytale Server.");
        System.out.println("======================================================");
    }
}
