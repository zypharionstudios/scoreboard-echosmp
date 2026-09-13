package de.echosmp.echosmp;

import de.echosmp.echosmp.listeners.JoinListener;
import de.echosmp.echosmp.listeners.QuitListener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Hauptklasse des EchoSMP-Plugins.
 * Startet alle Manager, Listener und wiederkehrenden Aufgaben.
 */
public final class EchoSmpPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private PlaytimeManager playtimeManager;
    private ScoreboardManager scoreboardManager;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);

        this.playtimeManager = new PlaytimeManager(this);
        this.playtimeManager.load();

        this.scoreboardManager = new ScoreboardManager(this, configManager, playtimeManager);

        getServer().getPluginManager().registerEvents(
                new JoinListener(playtimeManager, scoreboardManager), this
        );
        getServer().getPluginManager().registerEvents(
                new QuitListener(playtimeManager, scoreboardManager), this
        );

        long updateInterval = configManager.getUpdateIntervalTicks();
        getServer().getScheduler().runTaskTimer(
                this,
                () -> scoreboardManager.updateAll(),
                updateInterval,
                updateInterval
        );

        getServer().getScheduler().runTaskTimer(
                this,
                () -> scoreboardManager.updateRainbowAll(),
                2L,
                2L
        );

        getServer().getScheduler().runTaskTimer(
                this,
                () -> playtimeManager.saveAllOnline(),
                6000L,
                6000L
        );

        getLogger().info("EchoSMP wurde aktiviert.");
    }

    @Override
    public void onDisable() {
        if (playtimeManager != null) {
            playtimeManager.saveAllOnline();
        }
        getLogger().info("EchoSMP wurde deaktiviert.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PlaytimeManager getPlaytimeManager() {
        return playtimeManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }
}
