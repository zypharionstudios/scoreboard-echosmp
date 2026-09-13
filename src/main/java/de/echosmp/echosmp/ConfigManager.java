package de.echosmp.echosmp;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Verwaltet die automatisch erzeugte config.yml.
 * Es sind keine manuellen Reload- oder Toggle-Befehle nötig.
 */
public class ConfigManager {

    private final JavaPlugin plugin;

    private int updateIntervalTicks;
    private int maxPlayers;
    private String gradientStart;
    private String gradientMid;
    private String gradientEnd;
    private String emojiPlayer;
    private String emojiClock;
    private String scoreboardTitle;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;

        // Standard-Config aus den Ressourcen kopieren, falls sie noch nicht existiert
        plugin.saveDefaultConfig();

        FileConfiguration config = plugin.getConfig();

        // Standardwerte setzen, falls Schlüssel fehlen
        config.addDefault("update-interval-ticks", 20);
        config.addDefault("max-players", -1);
        config.addDefault("gradient-start", "#FF8A8A");
        config.addDefault("gradient-mid", "#E63946");
        config.addDefault("gradient-end", "#FFA500");
        config.addDefault("emoji-player", "👤");
        config.addDefault("emoji-clock", "⏰");
        config.addDefault("scoreboard-title", "echo smp");

        // Fehlende Werte ergänzen und speichern
        config.options().copyDefaults(true);
        plugin.saveConfig();

        loadValues();
    }

    private void loadValues() {
        FileConfiguration config = plugin.getConfig();
        this.updateIntervalTicks = config.getInt("update-interval-ticks", 20);
        this.maxPlayers = config.getInt("max-players", -1);
        this.gradientStart = config.getString("gradient-start", "#FF8A8A");
        this.gradientMid = config.getString("gradient-mid", "#E63946");
        this.gradientEnd = config.getString("gradient-end", "#FFA500");
        this.emojiPlayer = config.getString("emoji-player", "👤");
        this.emojiClock = config.getString("emoji-clock", "⏰");
        this.scoreboardTitle = config.getString("scoreboard-title", "echo smp");
    }

    public int getUpdateIntervalTicks() {
        return Math.max(1, updateIntervalTicks);
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public String getGradientStart() {
        return gradientStart;
    }

    public String getGradientMid() {
        return gradientMid;
    }

    public String getGradientEnd() {
        return gradientEnd;
    }

    public String getEmojiPlayer() {
        return emojiPlayer;
    }

    public String getEmojiClock() {
        return emojiClock;
    }

    public String getScoreboardTitle() {
        return scoreboardTitle;
    }
}
