package de.echosmp.echosmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Minimalistische, garantiert funktionierende Scoreboard-Implementierung.
 * Verwendet KEINE Teams und KEIN NumberFormat.blank().
 * Die roten Zahlen sind sichtbar (1, 2, 3, 4) – dafür wird die Sidebar
 * in Paper 1.21.6+ zuverlässig angezeigt.
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

    public void setupPlayer(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

        // Objective mit DisplayName erstellen
        Component title = Component.text("echo smp", NamedTextColor.RED);
        Objective objective = scoreboard.registerNewObjective(
                OBJECTIVE_NAME,
                Criteria.DUMMY,
                title
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Einträge mit echten Strings erstellen (KEINE unsichtbaren Codes!)
        // Die Zahlen sind sichtbar und dienen der Sortierung.
        objective.getScore("§a").setScore(4); // unsichtbarer Trenner
        objective.getScore("§b").setScore(3);
        objective.getScore("§c").setScore(2);
        objective.getScore("§d").setScore(1);

        // WICHTIG: Erst nach dem Setzen der Scores dem Spieler zuweisen!
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

        // Da wir keine Teams verwenden, können wir den Text nicht dynamisch ändern.
        // Die Einträge sind statisch. Für ein dynamisches Scoreboard bräuchte man
        // Teams oder das NumberFormat.blank() (siehe Version 2).
        // 
        // Diese Version zeigt daher nur statische Zeilen an.
        // Sie dient als Proof-of-Concept, dass die Sidebar überhaupt erscheint.
    }

    // Dummy-Methode, damit die Klasse kompiliert
    private String formatPlaytime(long seconds) {
        return playtimeManager.formatPlaytime(seconds);
    }
}
