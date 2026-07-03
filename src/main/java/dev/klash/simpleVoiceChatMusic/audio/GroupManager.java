package dev.klash.simpleVoiceChatMusic.audio;

import com.sedmelluq.discord.lavaplayer.filter.equalizer.EqualizerFactory;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.audiochannel.StaticAudioChannel;
import dev.klash.simpleVoiceChatMusic.SimpleVoiceChatMusic;
import dev.klash.simpleVoiceChatMusic.VoiceChatPlugin;
import dev.klash.simpleVoiceChatMusic.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.logging.Level;

import static dev.klash.simpleVoiceChatMusic.util.Constants.BASS_BOOST;

public class GroupManager {
    private final Group group;
    private final AudioPlayer lavaplayer;
    private final Server server;
    private final BlockingQueue<AudioTrack> queue;
    private final GroupSettingsManager settingsStore;

    private final ConcurrentHashMap<UUID, StaticAudioChannel> connections = new ConcurrentHashMap<>();
    private final MutableAudioFrame currentFrame;
    private final EqualizerFactory equalizer = new EqualizerFactory();

    private ScheduledFuture<?> audioFrameSendingTask = null;
    private ScheduledFuture<?> playerTrackingTask = null;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "SVCGroupMusicExecutor");
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler(
            (t, e) -> SimpleVoiceChatMusic.LOGGER.log(Level.SEVERE, "Uncaught exception in thread " + t.getName(), e)
        );

        return thread;
    });

    public GroupManager(Group group, AudioPlayer player, Server server) {
        this.group = group;
        this.server = server;
        this.lavaplayer = player;
        this.currentFrame = new MutableAudioFrame();
        this.settingsStore = GroupSettingsManager.getGroup(group);

        // apply EQ
        this.lavaplayer.setFilterFactory(this.equalizer);
        this.lavaplayer.setFrameBufferDuration(500);

        // buffer for storing current opus frame
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        currentFrame.setBuffer(buffer);

        // todo: max queue size
        this.queue = new LinkedBlockingQueue<>();

        // register events
        player.addListener(new TrackScheduler(this));

        // schedule task
        startGroupTracking();
        startAudioFrameSending();

        // restore settings
        this.setVolume(this.settingsStore.volume);
        this.setBassBoost(this.settingsStore.bassboost);
    }

    private void startAudioFrameSending() {
        if (this.audioFrameSendingTask != null && !this.audioFrameSendingTask.isDone()) {
            // already started, so leave it.
            SimpleVoiceChatMusic.LOGGER.info("Not starting new audio frame sending task.");
            return;
        }

        if (this.audioFrameSendingTask != null && this.audioFrameSendingTask.isDone()) {
            // stop and restart
            SimpleVoiceChatMusic.LOGGER.info("Frame task in stuck state, attempting to revive");
            this.audioFrameSendingTask.cancel(true);
        }

        SimpleVoiceChatMusic.LOGGER.info("Starting new audio frame sending task.");
        this.audioFrameSendingTask = this.executorService.scheduleAtFixedRate(() -> {
            if (VoiceChatPlugin.voicechatServerApi == null) {
                return;
            }

            // check if playback is paused
            if (this.lavaplayer == null || this.lavaplayer.isPaused() || this.lavaplayer.getPlayingTrack() == null) {
                return;
            }

            if (lavaplayer.provide(this.currentFrame)) {
                for (StaticAudioChannel channel : connections.values()) {
                    channel.send(this.currentFrame.getData());
                }
            }
        }, 1000L, 20L, TimeUnit.MILLISECONDS);
    }

    private void startGroupTracking() {
        this.playerTrackingTask = executorService.scheduleAtFixedRate(() -> {
            if (VoiceChatPlugin.voicechatServerApi == null) return;

            HashSet<UUID> uuids = new HashSet<>();

            for (Player serverPlayer : server.getOnlinePlayers()) {
                VoicechatConnection playerConnection = VoiceChatPlugin.voicechatServerApi.getConnectionOf(serverPlayer.getUniqueId());

                if (playerConnection == null || !playerConnection.isConnected()) continue;
                Group playerGroup = playerConnection.getGroup();
                if (playerGroup == null || playerGroup.getId() != this.group.getId()) continue;

                uuids.add(serverPlayer.getUniqueId());

                connections.computeIfAbsent(
                    serverPlayer.getUniqueId(),
                    (uuid) -> {
                        StaticAudioChannel channel = VoiceChatPlugin.voicechatServerApi.createStaticAudioChannel(
                            UUID.randomUUID()
                        );

                        if (channel == null) return null;
                        channel.addTarget(playerConnection);
                        channel.setCategory(VoiceChatPlugin.MUSIC_CATEGORY);

                        return channel;
                    }
                );
            }

            // now remove all that aren't here anymore
            for (UUID uuid : connections.keySet()) {
                if (uuids.contains(uuid)) continue;
                connections.remove(uuid);
            }

            // clean up if no players
            if (this.connections.isEmpty()) {
                SimpleVoiceChatMusic.LOGGER.log(Level.INFO, "Group {} is now empty. Cleaning up...", this.group.getName());
                this.cleanup();
            }

            // stop if no songs queued
            // if (this.lavaplayer.getPlayingTrack() == null && this.queue.isEmpty() && this.audioFrameSendingTask != null) {
            //     SimpleVoiceChatMusic.LOGGER.info("Pausing playback in {} due to empty queue", this.group.getName());
            //     this.audioFrameSendingTask.cancel(false);
            //     this.audioFrameSendingTask = null;
            // }
        }, 0L, 100L, TimeUnit.MILLISECONDS);
    }

    public boolean enqueueSong(AudioTrack track) {
        // noInterrupt true => false return if smth already playing
        //                     true return if nothing playing
        if (!lavaplayer.startTrack(track, true)) {
            return this.queue.offer(track);
        }

        return true;
    }

    public BlockingQueue<AudioTrack> getQueue() {
        return queue;
    }

    public void nextTrack() {
        // ensure this happens in the correct thread
        this.executorService.execute(() -> {
            // poll returns track or null
            // if null, lavaplayer stops
            AudioTrack track = queue.poll();
            lavaplayer.startTrack(track, false);

            // revive task if needed
            if (track != null) {
                this.startAudioFrameSending();
            } else {
                // no more songs to play, so quit
                this.cleanup();
            }
        });
    }

    public AudioPlayer getPlayer() {
        return this.lavaplayer;
    }

    public void broadcast(Component text) {
        // execute on main thread
        Bukkit.getScheduler().runTask(SimpleVoiceChatMusic.get(), ()->{
            Player[] players = server.getOnlinePlayers().stream().filter(
                    (player) -> this.connections.containsKey(player.getUniqueId())
            ).toArray(Player[]::new);

            for (Player player : players) {
                player.sendMessage(text);
            }
        });
    }

    public void cleanup() {
        this.broadcast(Text.literal("No more songs to play."));
        if (this.audioFrameSendingTask != null) this.audioFrameSendingTask.cancel(true);
        this.lavaplayer.destroy();
        MusicManager.getInstance().deleteGroup(this.group);
        if (this.playerTrackingTask != null) this.playerTrackingTask.cancel(false);
        this.executorService.shutdown();
    }

    public void setBassBoost(float percentage) {
        this.settingsStore.bassboost = percentage;
        final float multiplier = percentage / 100.00f;

        for (int i = 0; i < BASS_BOOST.length; i++) {
            this.equalizer.setGain(i, BASS_BOOST[i] * multiplier);
        }
    }

    public void setVolume(int volume) {
        this.settingsStore.volume = volume;
        this.getPlayer().setVolume(volume);
    }

    public final GroupSettingsManager getSettingsStore() {
        return this.settingsStore;
    }
}
