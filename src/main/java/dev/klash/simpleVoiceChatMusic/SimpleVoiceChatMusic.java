package dev.klash.simpleVoiceChatMusic;

import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import dev.klash.simpleVoiceChatMusic.audio.MusicManager;
import dev.klash.simpleVoiceChatMusic.commands.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public final class SimpleVoiceChatMusic extends JavaPlugin {

    public static Logger LOGGER;
    private final List<String> subCommands = Arrays.asList(
        "play", "search", "playing", "queue", "skip", 
        "pause", "resume", "stop", "volume", "kill", "bassboost"
    );

    public static SimpleVoiceChatMusic get() {
        return getPlugin(SimpleVoiceChatMusic.class);
    }

    public static boolean vcRegistered = false;

    VoiceChatPlugin plugin;

    @Override
    public void onEnable() {
        SimpleVoiceChatMusic.LOGGER = getLogger();

        BukkitVoicechatService service = getServer().getServicesManager().load(BukkitVoicechatService.class);
        if (service != null) {
            LOGGER.info("VC Service was not null! Attempting register");
            service.registerPlugin(plugin = new VoiceChatPlugin());
        }

        bassboost = new BassboostCommand();
        kill = new KillCommand();
        nowplaying = new NowPlayingCommand();
        pause = new PauseCommand();
        play = new PlayCommand();
        queue = new QueueCommand();
        resume = new ResumeCommand();
        search = new SearchCommand();
        skip = new SkipCommand();
        stop = new StopCommand();
        volume = new VolumeCommand();

        LOGGER.info("Loaded Simple Voice Chat Music!");
    }

    @Override
    public void onDisable() {
        LOGGER.info("Cleaning up due to shutdown.");
        MusicManager.getInstance().cleanup();
        if(plugin != null) {
            getServer().getServicesManager().unregister(plugin);
            LOGGER.info("Unregistered VoiceChat service.");
        }
    }

    dev.klash.simpleVoiceChatMusic.commands.Command
    bassboost, kill, nowplaying, pause, play, queue, resume, search, skip, stop, volume;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(label.equalsIgnoreCase("music_register_service")) {
            if(vcRegistered) {
                sender.sendMessage("VoiceChat service already registered!");
            }else {
                BukkitVoicechatService service = getServer().getServicesManager().load(BukkitVoicechatService.class);
                if (service != null) {
                    LOGGER.info("VC Service was not null!");
                    service.registerPlugin(new VoiceChatPlugin());
                    sender.sendMessage("VoiceChat service register attempted.");
                }else {
                    sender.sendMessage("VoiceChat service was null!");
                }
            }
            return true;
        }
        if(label.equalsIgnoreCase("music") || label.equalsIgnoreCase("svcmusic")) {
            if(args.length == 0) {
                sender.sendMessage("""
                    /music play <song title> - Searches and queues the first result
                    /music search <song> - Lists all results from YouTube (default) and lets you choose which you want to queue
                    /music now-playing - Displays the currently playing song
                    /music queue - Shows the queue
                    /music skip - Skips the current song
                    /music pause - Pauses the current song
                    /music resume - Resumes the current song
                    /music stop - Stops the current song and clears the queue
                    /music volume <1-100> - Sets the volume
                    /music kill - use when something goes wrong and you want to restart the plugin without restarting the server
                    /music bassboost <0-200> - adds bass boost
                """);
                return true;
            }else {
                try {
                    String[] argsWithoutCommand = new String[args.length - 1];
                    System.arraycopy(args, 1, argsWithoutCommand, 0, argsWithoutCommand.length);
                    switch(args[0]) {
                        case "play":
                            return play.execute((Player) sender, argsWithoutCommand) >= -1;
                        case "stop":
                            return stop.execute((Player) sender, argsWithoutCommand) >= -1;
                        case "pause":
                            return pause.execute((Player) sender, argsWithoutCommand) >= -1;
                        case "bassboost":
                            return bassboost.execute((Player) sender, argsWithoutCommand) >= -1;
                        case "kill":
                            return kill.execute((Player) sender, argsWithoutCommand) >= -1;
                        case "playing":
                            return nowplaying.execute((Player) sender, argsWithoutCommand) >= -1;
                        case "queue":
                            return queue.execute((Player) sender, argsWithoutCommand) >= -1;
                        case "resume":
                            return resume.execute((Player) sender, argsWithoutCommand) >= -1;
                        case "search":
                            return search.execute((Player) sender, argsWithoutCommand) >= -1;
                        case "skip":
                            return skip.execute((Player) sender, argsWithoutCommand) >= -1;
                        case "volume":
                            return volume.execute((Player) sender, argsWithoutCommand) >= -1;
                        default:
                            sender.sendMessage("/music <play/stop/pause/bassboost/kill/playing/queue/resume/search/skip/volume>");
                            return true;
                    }
                }catch(Exception e) {
                    sender.sendMessage("An error occurred while executing the command: " + e.getMessage() + " / " + e);
                    e.printStackTrace();
                    return true;
                }
            }
        }
        return super.onCommand(sender, command, label, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!alias.equalsIgnoreCase("music") && !alias.equalsIgnoreCase("svcmusic")) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], subCommands, completions);
            Collections.sort(completions);
            return completions;
        }
        return Collections.emptyList();
    }
}
