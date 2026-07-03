package dev.klash.simpleVoiceChatMusic.commands;

import dev.klash.simpleVoiceChatMusic.audio.SearchLoadHandler;
import dev.klash.simpleVoiceChatMusic.audio.GroupManager;
import dev.klash.simpleVoiceChatMusic.audio.MusicManager;
import dev.klash.simpleVoiceChatMusic.SimpleVoiceChatMusic;
import dev.klash.simpleVoiceChatMusic.util.ModUtils;
import dev.klash.simpleVoiceChatMusic.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.logging.Level;

import static dev.klash.simpleVoiceChatMusic.util.ModUtils.checkPlayerGroup;
import static dev.klash.simpleVoiceChatMusic.util.ModUtils.parseTrackId;

public class SearchCommand implements Command {
    public int execute(Player context, String[] args) throws Exception {
        final String query = parseTrackId(String.join(" ", args));

        ModUtils.CheckPlayerGroup result = checkPlayerGroup(context);
        if (result == null) return 1;

        SimpleVoiceChatMusic.LOGGER.log(Level.INFO, "Searching for " + query);

        Bukkit.getScheduler().runTask(SimpleVoiceChatMusic.get(), () -> {
            GroupManager gm = MusicManager.getInstance().getGroup(result.group(), result.source().getServer());
            result.source().sendMessage(() -> Text.literal("Loading songs..."));
            MusicManager.getInstance().playerManager.loadItemOrdered(
                gm.getPlayer(),
                query,
                new SearchLoadHandler(result.source(), gm)
            );
        });

        return 0;
    }

}
