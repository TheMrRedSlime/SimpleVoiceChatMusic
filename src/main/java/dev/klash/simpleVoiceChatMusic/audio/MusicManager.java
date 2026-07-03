package dev.klash.simpleVoiceChatMusic.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import de.maxhenkel.voicechat.api.Group;
import dev.klash.simpleVoiceChatMusic.SimpleVoiceChatMusic;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.klash.simpleVoiceChatMusic.util.YtDlpDownloader;
import com.github.topi314.lavasrc.ytdlp.YtdlpAudioSourceManager;
import java.nio.file.Path;
import org.bukkit.Server;

import java.util.HashMap;
import java.util.UUID;

public class MusicManager {
    private static final MusicManager instance = new MusicManager();
    public AudioPlayerManager playerManager;
    private final HashMap<UUID, GroupManager> groups = new HashMap<>();

    public MusicManager() {
        SimpleVoiceChatMusic.LOGGER.info("Loading sources...");
        this.playerManager = new DefaultAudioPlayerManager();

        // allow hotswapping EQ levels
        this.playerManager.getConfiguration().setFilterHotSwapEnabled(true);

        AudioSourceManagers.registerRemoteSources(
                this.playerManager,
                // we will load v2 of yt music player
                com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager.class
        );

        try {
            YtDlpDownloader downloader = new YtDlpDownloader(SimpleVoiceChatMusic.get());
            Path ytDlpPath = downloader.getOrDownloadYtDlp();
            
            YtdlpAudioSourceManager ytdlpSource = new YtdlpAudioSourceManager(
                ytDlpPath.toString(),
                10,
                25,
                1000,
                null,
                null
            );
            this.playerManager.registerSourceManager(ytdlpSource);
            SimpleVoiceChatMusic.LOGGER.info("yt-dlp source registered!");
        } catch (Exception e) {
            SimpleVoiceChatMusic.LOGGER.severe("Failed to register yt-dlp: " + e.getMessage());
        }

        //YoutubeAudioSourceManager ytSourceManager = new YoutubeAudioSourceManager();
        //this.playerManager.registerSourceManager(ytSourceManager);
        SimpleVoiceChatMusic.LOGGER.info("Loaded all sources!");
    }

    public static MusicManager getInstance() {
        return instance;
    }

    public GroupManager getGroup(Group group, Server server) {
        if (groups.containsKey(group.getId())) {
            return groups.get(group.getId());
        } else {
            GroupManager gm = new GroupManager(group, playerManager.createPlayer(), server);
            groups.put(group.getId(), gm);
            return gm;
        }
    }

    public GroupManager deleteGroup(Group group) {
        return groups.remove(group.getId());
    }

    /**
     * Destroys all groups
     */
    public void cleanup() {
        for (GroupManager gm : groups.values()) {
            gm.cleanup();
        }

        groups.clear();
    }
}