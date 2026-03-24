package hxgn;

import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.plugin.Plugin;

public class Main extends Plugin {
    @Override
    public void onLoad() {
        this.getLogger().info("AutoMender loaded");
        RusherHackAPI.getModuleManager().registerFeature(new AutoMend());
    }

    @Override
    public void onUnload() {
        this.getLogger().info("AutoMender unloaded");
    }
}