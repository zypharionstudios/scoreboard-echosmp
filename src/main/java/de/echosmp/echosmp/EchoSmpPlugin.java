package de.echosmp.echosmp;

import de.echosmp.echosmp.listeners.JoinListener;
import de.echosmp.echosmp.listeners.QuitListener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Hauptklasse des EchoSMP-Plugins.
 * Startet alle Manager, Listener und wiederkehrenden Aufgaben.
 * Es gibt keine Befehle – alles läuft vollautomatisch.
 */
public final class EchoSmpPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private PlaytimeManager playtimeManager;
    private ScoreboardManager scoreboardManager;

    @Override
    public void onEnable() {
        // Konfiguration automatisch laden/erzeugen
        this.configManager = new ConfigManager(this);

        // Spielzeiten laden
        this.playtimeManager = new PlaytimeManager(this);
        this.playtimeManager.load();

        // Scoreboard-Manager initialisieren
        this.scoreboardManager = new ScoreboardManager(this, configManager, playtimeManager);

        // Listener registrieren
        getServer().getPluginManager().registerEvents(
                new JoinListener(playtimeManager, scoreboardManager), this
        );
        getServer().getPluginManager().registerEvents(
                new QuitListener(playtimeManager, scoreboardManager), this
        );

        // Scoreboard jede Sekunde aktualisieren (Intervall aus config)
        long updateInterval = configManager.getUpdateIntervalTicks();
        getServer().getScheduler().runTaskTimer(
                this,
                () -> scoreboardManager.updateAll(),
                updateInterval,
                updateInterval
        );

        // Alle 5 Minuten (20 * 60 * 5 = 6000 Ticks) Spielzeiten sichern
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
        // Beim Serverstop alle Spielzeiten sichern
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
