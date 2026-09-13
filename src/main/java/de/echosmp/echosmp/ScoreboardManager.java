package de.echosmp.echosmp;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
 *
 * Layout (von oben nach unten):
 *   echo smp        (Gradient)
 *   Spielername     (weiß)
 *   👤 3/20         (blau)
 *   ⏰ 2h 15m       (gold)
 *   📶 23ms         (grün)
 *   ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬  (Trennstrich)
 *   discord:eosmp   (Discord-Blurple #5865F2)
 *
 * Rote Zahlen rechts werden mit NumberFormat.blank() ausgeblendet.
 */
public class ScoreboardManager {

    private static final String OBJECTIVE_NAME = "echo_smp";

    // Discord-Blurple
    private static final String DISCORD_BLURPLE = "#5865F2";

    // Trennstrich (16 Zeichen, füllt die Sidebar-Breite)
    private static final String SEPARATOR = "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final PlaytimeManager playtimeManager;
    private final Map<UUID, Scoreboard> scoreboards = new HashMap<>();

    // Hilfsserializer für MiniMessage → Legacy-String (für Scoreboard-Einträge)
    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacySection();
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public ScoreboardManager(JavaPlugin plugin, ConfigManager configManager, PlaytimeManager playtimeManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.playtimeManager = playtimeManager;
    }

    public void setupPlayer(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

        Objective objective = scoreboard.registerNewObjective(
                OBJECTIVE_NAME,
                Criteria.DUMMY,
                getTitleComponent()
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Wichtig für Paper 1.21.6+
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

        // Alte Einträge entfernen, damit sich bei Änderungen nichts stapelt
        for (String entry : new HashSet<>(scoreboard.getEntries())) {
            scoreboard.resetScores(entry);
        }

        // Werte berechnen
        int online = Bukkit.getOnlinePlayers().size();
        int max = configManager.getMaxPlayers() > 0
                ? configManager.getMaxPlayers()
                : Bukkit.getMaxPlayers();
        long seconds = playtimeManager.getPlaytimeSeconds(player.getUniqueId());
        int ping = player.getPing();

        // Zeile 1: Spielername (weiß) – Score 6
        objective.getScore(toLegacy("<white>" + player.getName())).setScore(6);

        // Zeile 2: 👤 Spielerzahl (blau) – Score 5
        objective.getScore(toLegacy("<blue>" + configManager.getEmojiPlayer() + " " + online + "/" + max))
                .setScore(5);

        // Zeile 3: ⏰ Spielzeit (gold) – Score 4
        objective.getScore(toLegacy("<gold>" + configManager.getEmojiClock() + " "
                + playtimeManager.formatPlaytime(seconds))).setScore(4);

        // Zeile 4: 📶 Ping in ms (grün) – Score 3
        objective.getScore(toLegacy("<green>📶 " + ping + "ms")).setScore(3);

        // Zeile 5: Trennstrich (dunkelgrau) – Score 2
        objective.getScore(toLegacy("<dark_gray>" + SEPARATOR)).setScore(2);

        // Zeile 6: discord:eosmp (Discord-Blurple) – Score 1
        objective.getScore(toLegacy("<color:" + DISCORD_BLURPLE + ">discord:eosmp")).setScore(1);
    }

    /**
     * Wandelt einen MiniMessage-String in einen Legacy-String um,
     * den das Scoreboard als Eintrag akzeptiert.
     */
    private String toLegacy(String miniMessage) {
        Component component = MM.deserialize(miniMessage);
        return LEGACY.serialize(component);
    }

    private Component getTitleComponent() {
        String gradient = "<gradient:"
                + configManager.getGradientStart() + ":"
                + configManager.getGradientMid() + ":"
                + configManager.getGradientEnd() + ">"
                + configManager.getScoreboardTitle()
                + "</gradient>";
        return MM.deserialize(gradient);
    }
}
