package org.clockworx.werewolf;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Bootstrap class for the Werewolf Paper plugin.
 * This class is called before the server starts and can be used to
 * set up the plugin environment before the main plugin class is instantiated.
 */
public class WerewolfBootstrap implements PluginBootstrap {

    /**
     * Called during the bootstrap phase, before the server starts.
     * Currently, this is a placeholder for future bootstrap functionality.
     *
     * @param context The bootstrap context provided by Paper.
     */
    @Override
    public void bootstrap(BootstrapContext context) {
        // Bootstrap logic can be added here in the future
        // For now, we don't need any bootstrap-specific setup
    }

    /**
     * Creates and returns the main plugin instance.
     * This is called after bootstrap and allows passing custom parameters
     * to the plugin constructor.
     *
     * @param context The plugin provider context.
     * @return The main WerewolfPlugin instance.
     */
    @Override
    public JavaPlugin createPlugin(PluginProviderContext context) {
        return new WerewolfPlugin();
    }
}

