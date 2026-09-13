package de.echosmp.echosmp;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verwaltet die persistente Spielzeit aller Spieler.
 * Speicherort: plugins/EchoSMP/playerdata.yml
 * Format: UUID: <Sekunden>
 */
public class PlaytimeManager {

    private final JavaPlugin plugin;
    private final File file;

    // Gespeicherte Spielzeit in Sekunden (auch für offline Spieler)
    private final Map<UUID, Long> playtimeSeconds = new ConcurrentHashMap<>();

    // Startzeitpunkt der aktuellen Session (nur für Online-Spieler)
    private final Map<UUID, Long> sessionStartMillis = new ConcurrentHashMap<>();

    public PlaytimeManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "playerdata.yml");
    }

    /**
     * Lädt alle Spielzeiten aus der playerdata.yml.
     */
    public void load() {
        if (!file.exists()) {
            return;
        }

        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        for (String key : data.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                long seconds = data.getLong(key, 0L);
                playtimeSeconds.put(uuid, seconds);
            } catch (IllegalArgumentException ignored) {
                // Ungültige UUIDs überspringen
            }
        }
    }

    /**
     * Speichert alle bekannten Spielzeiten in die playerdata.yml.
     */
    public void save() {
        YamlConfiguration data = new YamlConfiguration();

        for (Map.Entry<UUID, Long> entry : playtimeSeconds.entrySet()) {
            data.set(entry.getKey().toString(), entry.getValue());
        }

        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Konnte playerdata.yml nicht speichern: " + e.getMessage());
        }
    }

    /**
     * Wird beim Join aufgerufen: Session starten.
     */
    public void handleJoin(Player player) {
        UUID uuid = player.getUniqueId();
        playtimeSeconds.putIfAbsent(uuid, 0L);
        sessionStartMillis.put(uuid, System.currentTimeMillis());
    }

    /**
     * Wird beim Quit aufgerufen: aktuelle Session addieren und sofort speichern.
     */
    public void handleQuit(Player player) {
        UUID uuid = player.getUniqueId();
        updateOnlinePlaytime(uuid);
        sessionStartMillis.remove(uuid);
        save();
    }

    /**
     * Sichert alle Online-Spieler (z. B. alle 5 Minuten und beim Serverstop).
     */
    public void saveAllOnline() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateOnlinePlaytime(player.getUniqueId());
        }
        save();
    }

    /**
     * Addiert die vergangene Zeit seit dem letzten Update zur gespeicherten Spielzeit.
     */
    private void updateOnlinePlaytime(UUID uuid) {
        Long start = sessionStartMillis.get(uuid);
        if (start == null) {
            return;
        }

        long now = System.currentTimeMillis();
        long elapsedSeconds = (now - start) / 1000L;

        if (elapsedSeconds > 0) {
            playtimeSeconds.merge(uuid, elapsedSeconds, Long::sum);
            sessionStartMillis.put(uuid, now);
        }
    }

    /**
     * Gibt die aktuelle Gesamtspielzeit in Sekunden zurück.
     * Beinhaltet die laufende Session, falls der Spieler online ist.
     */
    public long getPlaytimeSeconds(UUID uuid) {
        long base = playtimeSeconds.getOrDefault(uuid, 0L);
        Long start = sessionStartMillis.get(uuid);

        if (start != null) {
            long now = System.currentTimeMillis();
            base += (now - start) / 1000L;
        }

        return base;
    }

    /**
     * Formatiert die Spielzeit:
     * - unter 1 Minute: Xs
     * - unter 1 Stunde: Ym
     * - ab 1 Stunde: Xh Ym
     */
    public String formatPlaytime(long totalSeconds) {
        if (totalSeconds < 60) {
            return totalSeconds + "s";
        }

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;

        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }

        return minutes + "m";
    }
}
