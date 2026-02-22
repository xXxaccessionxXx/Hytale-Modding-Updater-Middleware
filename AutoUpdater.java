package com.hytale.updater.agent;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class AutoUpdater {
    private static final String GITHUB_REPO = "kasey/Hytale-Modding-Updater-Middleware"; // Make sure to replace with the actual GitHub username/repo
    private static final String API_URL = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";
    private static final String ASSET_NAME = "middleware-agent.jar";

    public static void checkForUpdates(String currentVersion) {
        System.out.println("\u001B[36m[Hytale Middleware Updater]\u001B[0m Checking for updates (Current: " + currentVersion + ")...");
        try {
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            // Set User-Agent as required by GitHub API
            connection.setRequestProperty("User-Agent", "HytaleMiddlewareUpdater");

            if (connection.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                String json = response.toString();
                // Simple JSON parsing without external dependencies
                String latestVersion = extractJsonValue(json, "\"tag_name\":");
                if (latestVersion == null) {
                    System.err.println("[Hytale Middleware Updater] Failed to parse latest version.");
                    return;
                }

                if (!latestVersion.equals(currentVersion)) {
                    System.out.println("\u001B[33m[Hytale Middleware Updater]\u001B[0m Update found: " + latestVersion + ". Downloading...");
                    String browserDownloadUrl = extractJsonValue(json, "\"browser_download_url\":");
                    if (browserDownloadUrl != null && browserDownloadUrl.endsWith(ASSET_NAME)) {
                        downloadUpdateAndScheduleReplace(browserDownloadUrl);
                    } else {
                        // Fallback: trying to find the asset URL manually if the first one wasn't right
                        int assetIndex = json.indexOf("\"name\":\"" + ASSET_NAME + "\"");
                        if (assetIndex != -1) {
                            String substr = json.substring(assetIndex);
                            browserDownloadUrl = extractJsonValue(substr, "\"browser_download_url\":");
                            if (browserDownloadUrl != null) {
                                downloadUpdateAndScheduleReplace(browserDownloadUrl);
                            }
                        }
                    }
                } else {
                    System.out.println("\u001B[32m[Hytale Middleware Updater]\u001B[0m Middleware is up to date (" + currentVersion + ").");
                }
            } else {
                System.err.println("[Hytale Middleware Updater] Failed to check for updates. HTTP Code: " + connection.getResponseCode());
            }
        } catch (Exception e) {
            System.err.println("[Hytale Middleware Updater] Error checking for updates: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void downloadUpdateAndScheduleReplace(String downloadUrl) throws Exception {
        URL url = new URL(downloadUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "HytaleMiddlewareUpdater");

        File targetFile = new File("middleware-agent-new.jar");
        try (InputStream in = connection.getInputStream()) {
            Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        System.out.println("\u001B[32m[Hytale Middleware Updater]\u001B[0m Download complete. Scheduling replacement on exit...");

        // Start apply_update.bat which will replace the jar when this process exits.
        File applyBat = new File("apply_update.bat");
        if (applyBat.exists()) {
            Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", "/b", applyBat.getAbsolutePath(), String.valueOf(ProcessHandle.current().pid())});
        } else {
            System.err.println("[Hytale Middleware Updater] apply_update.bat not found. Cannot schedule replacement.");
        }
    }

    private static String extractJsonValue(String json, String key) {
        int index = json.indexOf(key);
        if (index == -1) return null;
        int valueStart = json.indexOf("\"", index + key.length()) + 1;
        int valueEnd = json.indexOf("\"", valueStart);
        if (valueStart > 0 && valueEnd > valueStart) {
            return json.substring(valueStart, valueEnd);
        }
        return null;
    }
}
