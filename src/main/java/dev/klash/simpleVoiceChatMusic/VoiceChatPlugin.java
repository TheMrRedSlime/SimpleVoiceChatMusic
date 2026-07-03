package dev.klash.simpleVoiceChatMusic;

import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.VolumeCategory;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import org.apache.http.Header;
import org.apache.http.HttpEntity;

public class VoiceChatPlugin implements VoicechatPlugin {

    public static String MUSIC_CATEGORY = "streamed_music";

    public static VoicechatApi voicechatApi;
    public static VoicechatServerApi voicechatServerApi;
    public static VolumeCategory musicVolumeCategory;

    @Override
    public String getPluginId() {
        return "simplevoice_paper_chat_music";
    }

    @Override
    public void initialize(VoicechatApi api) {
        SimpleVoiceChatMusic.LOGGER.info("Voicechat API initialized!");
        voicechatApi = api;
        SimpleVoiceChatMusic.vcRegistered = true;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStart);
    }

    private void onServerStart(VoicechatServerStartedEvent event) {
        voicechatServerApi = event.getVoicechat();
        musicVolumeCategory = voicechatServerApi.volumeCategoryBuilder()
                .setId(MUSIC_CATEGORY)
                .setName("Music")
                .setDescription("The volume of streamed music.")
                .build();

        voicechatServerApi.registerVolumeCategory(musicVolumeCategory);
    }

}
