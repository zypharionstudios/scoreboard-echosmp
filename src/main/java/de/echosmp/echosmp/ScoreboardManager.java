package de.echosmp.echosmp;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Erstellt und aktualisiert das Sidebar-Scoreboard für jeden Spieler.
 * Die roten Zahlen rechts werden über die Paper-API ausgeblendet.
 */
public class ScoreboardManager {

    private static final String OBJECTIVE_NAME = "echo_smp";

    private static final String TEAM_PLAYER = "echo_line_player";
    private static final String TEAM_ONLINE = "echo_line_online";
    private static final String TEAM_PLAYTIME = "echo_line_playtime";

    // Unsichtbare, eindeutige Einträge für die Teams
    private static final String ENTRY_PLAYER = ChatColor.BLACK.toString();       // §0
    private static final String ENTRY_ONLINE = ChatColor.DARK_BLUE.toString();   // §1
    private static final String ENTRY_PLAYTIME = ChatColor.DARK_GREEN.toString(); // §2

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
     * Erstellt für einen Spieler ein neues Scoreboard und zeigt es an.
     */
    public void setupPlayer(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

        Objective objective = scoreboard.registerNewObjective(
                OBJECTIVE_NAME,
                Criteria.DUMMY,
                getTitleComponent()
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Rote Zahlen rechts ausblenden (Paper-API)
        objective.numberFormat(NumberFormat.blank());

        // Teams für die einzelnen Zeilen anlegen
        Team playerTeam = scoreboard.registerNewTeam(TEAM_PLAYER);
        playerTeam.addEntry(ENTRY_PLAYER);

        Team onlineTeam = scoreboard.registerNewTeam(TEAM_ONLINE);
        onlineTeam.addEntry(ENTRY_ONLINE);

        Team playtimeTeam = scoreboard.registerNewTeam(TEAM_PLAYTIME);
        playtimeTeam.addEntry(ENTRY_PLAYTIME);

        // Scores dienen nur der Sortierung (höher = weiter oben)
        objective.getScore(ENTRY_PLAYER).setScore(3);
        objective.getScore(ENTRY_ONLINE).setScore(2);
        objective.getScore(ENTRY_PLAYTIME).setScore(1);

        player.setScoreboard(scoreboard);
        scoreboards.put(player.getUniqueId(), scoreboard);

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

        // Falls das Scoreboard fehlt (z. B. nach Reload), neu aufbauen
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

        Component onlineComponent = Component.text(
                configManager.getEmojiPlayer() + " " + online + "/" + max,
                NamedTextColor.GRAY
        );
        onlineTeam.prefix(onlineComponent);

        // Zeile 4: Spielzeit
        long seconds = playtimeManager.getPlaytimeSeconds(player.getUniqueId());
        String playtime = playtimeManager.formatPlaytime(seconds);

        Component playtimeComponent = Component.text(
                configManager.getEmojiClock() + " " + playtime,
                NamedTextColor.GRAY
        );
        playtimeTeam.prefix(playtimeComponent);

        // Zusätzlich für alle Scores im Objective die Zahlen ausblenden
        for (String entry : objective.getScoreboard().getEntries()) {
            Score score = objective.getScore(entry);
            if (score != null) {
                score.setNumberFormat(NumberFormat.blank());
            }
        }
    }

    /**
     * Baut den Gradienten-Titel mit MiniMessage.
     */
    private Component getTitleComponent() {
        String title = configManager.getScoreboardTitle();

        String gradient = "<gradient:"
                + configManager.getGradientStart() + ":"
                + configManager.getGradientMid() + ":"
                + configManager.getGradientEnd() + ">"
                + title
                + "</gradient>";

        return MiniMessage.miniMessage().deserialize(gradient);
    }
}
