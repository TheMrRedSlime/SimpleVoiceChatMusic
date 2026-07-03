package dev.klash.simpleVoiceChatMusic.util;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class YtDlpDownloader {
    // A auto yt-dlp downloader that SHOULD auto update itself
    private static final String GITHUB_RELEASE = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/";
    private final JavaPlugin plugin;
    private Path ytDlpPath;

    public YtDlpDownloader(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public Path getOrDownloadYtDlp() throws IOException {
        Path configDir = plugin.getDataFolder().toPath();
        if (!Files.exists(configDir)) {
            Files.createDirectories(configDir);
        }

        String fileName = getFileNameForOS();
        ytDlpPath = configDir.resolve(fileName);

        if (Files.exists(ytDlpPath)) {
            plugin.getLogger().info("yt-dlp found at: " + ytDlpPath);
            runYtDlpUpdater();
            return ytDlpPath;
        }

        plugin.getLogger().info("Downloading yt-dlp to: " + ytDlpPath);
        String downloadUrl = GITHUB_RELEASE + fileName;
        
        try (InputStream in = new URL(downloadUrl).openStream()) {
            Files.copy(in, ytDlpPath, StandardCopyOption.REPLACE_EXISTING);
        }

        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            ytDlpPath.toFile().setExecutable(true);
        }

        plugin.getLogger().info("yt-dlp downloaded successfully!");
        return ytDlpPath;
    }

    private String getFileNameForOS() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();

        if (os.contains("win")) {
            return "yt-dlp.exe";
        } else if (os.contains("mac")) {
            return arch.contains("arm") ? "yt-dlp_arm64" : "yt-dlp_macos";
        } else {
            return arch.contains("arm") ? "yt-dlp_linux_armv7l" : "yt-dlp";
        }
    }

    private void runYtDlpUpdater() {
    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
        try {
            ProcessBuilder pb = new ProcessBuilder(ytDlpPath.toAbsolutePath().toString(), "-U");
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    plugin.getLogger().info("[yt-dlp Updater] " + line);
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                plugin.getLogger().info("yt-dlp update check completed successfully.");
            } else {
                plugin.getLogger().warning("yt-dlp updater exited with code: " + exitCode);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to run yt-dlp updater: " + e.getMessage());
        }
    });
}
}