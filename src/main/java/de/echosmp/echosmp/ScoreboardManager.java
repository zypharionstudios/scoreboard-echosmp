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
 * Scoreboard mit klassischen Minecraft-Farbcodes (§).
 * Keine Hex-Farben, kein Regenbogen – alles zuverlässig sichtbar.
 *
 * Layout:
 *   echo smp        (Gradient-Titel)
 *   Spielername     (§f weiß)
 *   👤  1/20        (§b hellblau)
 *   ⏰  39m         (§6 gold)
 *   💰  Coming soon (§a grün)
 *   📶  57ms        (§a / §6 / §c je nach Ping)
 *   ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬  (§0 schwarz)
 *   discord:eosmp   (§5 dunkel lila)
 */
public class ScoreboardManager {

    private static final String OBJECTIVE_NAME = "echo_smp";

    // Einheitlicher Abstand nach jedem Emoji
    private static final String SPACING = "  ";

    private static final String SEPARATOR = "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final PlaytimeManager playtimeManager;
    private final Map<UUID, Scoreboard> scoreboards = new HashMap<>();

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
        objective.setAutoUpdateDisplay(true);
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

    /** Nicht mehr benötigt – vorhanden, damit EchoSmpPlugin nicht angepasst werden muss. */
    public void updateRainbowAll() {
        // no-op
    }

    public void updatePlayer(Player player) {
        Scoreboard scoreboard = scoreboards.get(player.getUniqueId());
        if (scoreboard == null) { setupPlayer(player); return; }

        Objective objective = scoreboard.getObjective(OBJECTIVE_NAME);
        if (objective == null) { setupPlayer(player); return; }

        // Alle alten Einträge entfernen
        for (String entry : new HashSet<>(scoreboard.getEntries())) {
            scoreboard.resetScores(entry);
        }

        int online = Bukkit.getOnlinePlayers().size();
        int max = configManager.getMaxPlayers() > 0
                ? configManager.getMaxPlayers()
                : Bukkit.getMaxPlayers();
        long seconds = playtimeManager.getPlaytimeSeconds(player.getUniqueId());
        int ping = player.getPing();

        // Score 7 = Name, 6 = 👤, 5 = ⏰, 4 = 💰, 3 = 📶, 2 = Striche, 1 = Discord
        objective.getScore("§f" + player.getName()).setScore(7);

        objective.getScore("§b" + configManager.getEmojiPlayer() + SPACING
                + online + "/" + max).setScore(6);

        objective.getScore("§6" + configManager.getEmojiClock() + SPACING
                + playtimeManager.formatPlaytime(seconds)).setScore(5);

        objective.getScore("§a💰" + SPACING + "Coming soon").setScore(4);

        objective.getScore(getPingColor(ping) + "📶" + SPACING + ping + "ms").setScore(3);

        objective.getScore("§0" + SEPARATOR).setScore(2);

        objective.getScore("§5discord:eosmp").setScore(1);
    }

    /**
     * Ping-Farben als klassische Minecraft-Codes:
     *   1–150   → §a grün
     *   151–250 → §6 orange
     *   251+    → §c rot
     */
    private String getPingColor(int ping) {
        if (ping <= 150) {
            return "§a";
        } else if (ping <= 250) {
            return "§6";
        } else {
            return "§c";
        }
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
