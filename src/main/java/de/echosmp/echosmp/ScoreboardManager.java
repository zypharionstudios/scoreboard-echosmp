package de.echosmp.echosmp;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

/**
 * Scoreboard-Implementierung ohne Teams.
 * Die Einträge SIND der sichtbare Text – kein Prefix, kein Rest dahinter.
 *
 * Farben:
 *   - Name: weiß (§f)
 *   - 👤 Spielerzahl: blau (§b)
 *   - ⏰ Spielzeit: gold (§6)
 */
public class ScoreboardManager {

    private static final String OBJECTIVE_NAME = "echo_smp";

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final PlaytimeManager playtimeManager;
    private final Map<UUID, Scoreboard> scoreboards = new HashMap<>();

    public ScoreboardManager(JavaPlugin plugin, ConfigManager configManager, PlaytimeManager playtimeManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.playtimeManager = playtimeManager;
    }

    /**
     * Baut das Scoreboard für einen Spieler auf.
     */
    public void setupPlayer(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

        // Objective mit Gradient-Titel
        Objective objective = scoreboard.registerNewObjective(
                OBJECTIVE_NAME,
                Criteria.DUMMY,
                getTitleComponent()
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Wichtig für Paper 1.21.6+ – sonst wird die Sidebar nicht gerendert
        objective.setAutoUpdateDisplay(true);

        // Rote Zahlen rechts ausblenden
        objective.numberFormat(NumberFormat.blank());

        player.setScoreboard(scoreboard);
        scoreboards.put(player.getUniqueId(), scoreboard);

        updatePlayer(player);
    }

    public void removePlayer(Player player) {
        scoreboards.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
        }
    }

    /**
     * Aktualisiert die drei Zeilen. Alte Einträge werden gelöscht,
     * damit sich bei Änderungen (z. B. Spielzeit) nichts doppelt.
     */
    public void updatePlayer(Player player) {
        Scoreboard scoreboard = scoreboards.get(player.getUniqueId());

        if (scoreboard == null) {
            setupPlayer(player);
            return;
        }

        Objective objective = scoreboard.getObjective(OBJECTIVE_NAME);
        if (objective == null) {
            setupPlayer(player);
            return;
        }

        // Alle alten Einträge entfernen
        for (String entry : new HashSet<>(scoreboard.getEntries())) {
            scoreboard.resetScores(entry);
        }

        // Werte berechnen
        int online = Bukkit.getOnlinePlayers().size();
        int max = configManager.getMaxPlayers() > 0
                ? configManager.getMaxPlayers()
                : Bukkit.getMaxPlayers();
        long seconds = playtimeManager.getPlaytimeSeconds(player.getUniqueId());

        // Zeile 1: Spielername (weiß)
        objective.getScore("§f" + player.getName()).setScore(3);

        // Zeile 2: 👤 Spielerzahl (blau)
        objective.getScore("§b" + configManager.getEmojiPlayer() + " " + online + "/" + max).setScore(2);

        // Zeile 3: ⏰ Spielzeit (gold)
        objective.getScore("§6" + configManager.getEmojiClock() + " " + playtimeManager.formatPlaytime(seconds)).setScore(1);
    }

    /**
     * Baut den Gradienten-Titel mit MiniMessage.
     */
    private Component getTitleComponent() {
        String gradient = "<gradient:"
                + configManager.getGradientStart() + ":"
                + configManager.getGradientMid() + ":"
                + configManager.getGradientEnd() + ">"
                + configManager.getScoreboardTitle()
                + "</gradient>";
        return MiniMessage.miniMessage().deserialize(gradient);
    }
}
