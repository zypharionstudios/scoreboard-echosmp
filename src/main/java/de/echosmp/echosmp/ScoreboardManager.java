package de.echosmp.echosmp;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Finale, funktionierende Scoreboard-Implementierung für Paper 1.21+.
 *
 * Verwendet Teams für die Zeilen und NumberFormat.blank() zum Ausblenden
 * der roten Zahlen. Entscheidend ist die Reihenfolge:
 *   1. Objective erstellen + DisplaySlot setzen
 *   2. Teams erstellen + Entries hinzufügen
 *   3. ERST DANN Scores setzen
 *   4. NumberFormat.blank() auf dem Objective setzen
 */
public class ScoreboardManager {

    private static final String OBJECTIVE_NAME = "echo_smp";

    // Team-Namen (intern, nicht sichtbar)
    private static final String TEAM_PLAYER = "line_player";
    private static final String TEAM_ONLINE = "line_online";
    private static final String TEAM_PLAYTIME = "line_playtime";

    // Entry-Strings – müssen normale, eindeutige Strings sein.
    // Der sichtbare Inhalt kommt über den Team-Prefix.
    private static final String ENTRY_PLAYER = "entry_player";
    private static final String ENTRY_ONLINE = "entry_online";
    private static final String ENTRY_PLAYTIME = "entry_playtime";

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

        // --- 1. Objective erstellen und DisplaySlot setzen ---
        Objective objective = scoreboard.registerNewObjective(
                OBJECTIVE_NAME,
                Criteria.DUMMY,
                getTitleComponent()
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // --- 2. Teams ZUERST erstellen und Entries hinzufügen ---
        Team playerTeam = scoreboard.registerNewTeam(TEAM_PLAYER);
        playerTeam.addEntry(ENTRY_PLAYER);

        Team onlineTeam = scoreboard.registerNewTeam(TEAM_ONLINE);
        onlineTeam.addEntry(ENTRY_ONLINE);

        Team playtimeTeam = scoreboard.registerNewTeam(TEAM_PLAYTIME);
        playtimeTeam.addEntry(ENTRY_PLAYTIME);

        // --- 3. ERST JETZT Scores setzen (nachdem Teams existieren!) ---
        // Höherer Score = weiter oben in der Sidebar
        objective.getScore(ENTRY_PLAYER).setScore(3);
        objective.getScore(ENTRY_ONLINE).setScore(2);
        objective.getScore(ENTRY_PLAYTIME).setScore(1);

        // --- 4. NumberFormat.blank() auf dem Objective setzen ---
        // Blendet die roten Zahlen rechts aus (Paper 1.20.3+)
        objective.numberFormat(NumberFormat.blank());

        // Scoreboard dem Spieler zuweisen
        player.setScoreboard(scoreboard);
        scoreboards.put(player.getUniqueId(), scoreboard);

        // Sofort mit aktuellen Werten befüllen
        updatePlayer(player);
    }

    /**
     * Entfernt das Scoreboard beim Quit.
     */
    public void removePlayer(Player player) {
        scoreboards.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    /**
     * Aktualisiert das Scoreboard für alle Online-Spieler.
     */
    public void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
        }
    }

    /**
     * Aktualisiert die Zeilen für einen einzelnen Spieler.
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

        Team playerTeam = scoreboard.getTeam(TEAM_PLAYER);
        Team onlineTeam = scoreboard.getTeam(TEAM_ONLINE);
        Team playtimeTeam = scoreboard.getTeam(TEAM_PLAYTIME);

        if (playerTeam == null || onlineTeam == null || playtimeTeam == null) {
            setupPlayer(player);
            return;
        }

        // Zeile 2: Spielername
        playerTeam.prefix(Component.text(player.getName(), NamedTextColor.WHITE));

        // Zeile 3: Online-Spieler
        int online = Bukkit.getOnlinePlayers().size();
        int max = configManager.getMaxPlayers() > 0
                ? configManager.getMaxPlayers()
                : Bukkit.getMaxPlayers();
        onlineTeam.prefix(Component.text(
                configManager.getEmojiPlayer() + " " + online + "/" + max,
                NamedTextColor.GRAY
        ));

        // Zeile 4: Spielzeit
        long seconds = playtimeManager.getPlaytimeSeconds(player.getUniqueId());
        playtimeTeam.prefix(Component.text(
                configManager.getEmojiClock() + " " + playtimeManager.formatPlaytime(seconds),
                NamedTextColor.GRAY
        ));
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
