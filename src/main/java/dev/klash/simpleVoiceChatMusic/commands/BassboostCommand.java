package dev.klash.simpleVoiceChatMusic.commands;

import dev.klash.simpleVoiceChatMusic.SimpleVoiceChatMusic;
import dev.klash.simpleVoiceChatMusic.audio.GroupManager;
import dev.klash.simpleVoiceChatMusic.audio.MusicManager;
import dev.klash.simpleVoiceChatMusic.util.ModUtils;
import dev.klash.simpleVoiceChatMusic.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import static dev.klash.simpleVoiceChatMusic.util.ModUtils.checkPlayerGroup;

public class BassboostCommand implements Command {
    public int execute(Player player, String[] args) throws Exception {
        if (args.length != 1) {
            player.sendMessage(() -> Text.literal("Usage: /music bassboost <0-100>"));
            return 1;
        }
        
        try {
            Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(() -> Text.literal("Invalid input. Please enter a number between 0 and 100."));
            return 0;
        }

        float bass = Float.parseFloat(args[0]);
        ModUtils.CheckPlayerGroup result = checkPlayerGroup(player);
        if (result == null) return 1;

        Bukkit.getScheduler().runTask(SimpleVoiceChatMusic.get(), () -> {
            GroupManager gm = MusicManager.getInstance().getGroup(result.group(), result.source().getServer());
            gm.broadcast(Text.literal("Bassboost set to " + bass + "% by " + player.getName()));
            gm.setBassBoost(bass);
        });

        return 0;
    }

}
