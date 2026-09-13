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
 * Scoreboard mit animiertem Regenbogen-Trennstrich.
 *
 * Layout:
 *   echo smp        (Gradient)
 *   Spielername     (weiß)
 *   👤  3/20        (blau)
 *   ⏰  2h 15m      (gold)
 *   💰  Coming soon (grün)
 *   📶  23ms        (dynamisch: grün / orange / rot je nach Ping)
 *   ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬  (Regenbogen, wandert nach rechts)
 *   discord:eosmp   (Discord-Blurple)
 *
 * Alle Emoji-Zeilen haben dieselbe Einrückung (3 Leerzeichen nach Emoji),
 * damit die Werte bündig untereinander starten.
 */
public class ScoreboardManager {

    private static final String OBJECTIVE_NAME = "echo_smp";
    private static final String DISCORD_BLURPLE = "#5865F2";

    // Feste Farben
    private static final String COLOR_GREEN = "#55FF55";
    private static final String COLOR_ORANGE = "#FFA500";
    private static final String COLOR_RED = "#FF5555";

    // Einheitlicher Abstand nach jedem Emoji für bündige Ausrichtung
    private static final String SPACING = "   "; // 3 Leerzeichen

    private static final String SEPARATOR_CHARS = "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";
    private static final float RAINBOW_SPEED = 0.15f;

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final PlaytimeManager playtimeManager;
    private final Map<UUID, Scoreboard> scoreboards = new HashMap<>();
    private final Map<UUID, String> currentSeparators = new HashMap<>();

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
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
        currentSeparators.remove(player.getUniqueId());

        updatePlayer(player);
        updateRainbow(player);
    }

    public void removePlayer(Player player) {
        scoreboards.remove(player.getUniqueId());
        currentSeparators.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
        }
    }

    public void updateRainbowAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateRainbow(player);
        }
    }

    public void updatePlayer(Player player) {
        Scoreboard scoreboard = scoreboards.get(player.getUniqueId());
        if (scoreboard == null) { setupPlayer(player); return; }

        Objective objective = scoreboard.getObjective(OBJECTIVE_NAME);
        if (objective == null) { setupPlayer(player); return; }

        String currentSep = currentSeparators.get(player.getUniqueId());
        for (String entry : new HashSet<>(scoreboard.getEntries())) {
            if (!entry.equals(currentSep)) {
                scoreboard.resetScores(entry);
            }
        }

        int online = Bukkit.getOnlinePlayers().size();
        int max = configManager.getMaxPlayers() > 0
                ? configManager.getMaxPlayers()
                : Bukkit.getMaxPlayers();
        long seconds = playtimeManager.getPlaytimeSeconds(player.getUniqueId());
        int ping = player.getPing();

        // Score 7 = Name, 6 = 👤, 5 = ⏰, 4 = 💰, 3 = 📶, 2 = Regenbogen, 1 = Discord
        objective.getScore(toLegacy("<white>" + player.getName())).setScore(7);

        objective.getScore(toLegacy("<blue>"
                + configManager.getEmojiPlayer() + SPACING
                + online + "/" + max)).setScore(6);

        objective.getScore(toLegacy("<gold>"
                + configManager.getEmojiClock() + SPACING
                + playtimeManager.formatPlaytime(seconds))).setScore(5);

        objective.getScore(toLegacy("<" + COLOR_GREEN + ">"
                + "💰" + SPACING
                + "Coming soon")).setScore(4);

        objective.getScore(toLegacy("<" + getPingColor(ping) + ">"
                + "📶" + SPACING
                + ping + "ms")).setScore(3);

        objective.getScore(toLegacy("<color:" + DISCORD_BLURPLE + ">discord:eosmp")).setScore(1);
    }

    /**
     * Wählt die Ping-Farbe:
     *   1–150 ms   → grün
     *   151–250 ms → orange
     *   251+ ms    → rot
     */
    private String getPingColor(int ping) {
        if (ping <= 150) {
            return COLOR_GREEN;
        } else if (ping <= 250) {
            return COLOR_ORANGE;
        } else {
            return COLOR_RED;
        }
    }

    public void updateRainbow(Player player) {
        Scoreboard scoreboard = scoreboards.get(player.getUniqueId());
        if (scoreboard == null) return;

        Objective objective = scoreboard.getObjective(OBJECTIVE_NAME);
        if (objective == null) return;

        String old = currentSeparators.remove(player.getUniqueId());
        if (old != null) {
            scoreboard.resetScores(old);
        }

        String newSep = buildRainbowSeparator();
        objective.getScore(newSep).setScore(2);
        currentSeparators.put(player.getUniqueId(), newSep);
    }

    private String buildRainbowSeparator() {
        long tick = System.currentTimeMillis() / 50L;
        int len = SEPARATOR_CHARS.length();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < len; i++) {
            float hue = ((i - tick * RAINBOW_SPEED) / len) % 1.0f;
            if (hue < 0) hue += 1.0f;

            int rgb = hsvToRgb(hue, 1.0f, 1.0f);
            String hex = String.format("#%06X", rgb & 0xFFFFFF);
            sb.append("<color:").append(hex).append(">").append(SEPARATOR_CHARS.charAt(i));
        }

        return toLegacy(sb.toString());
    }

    private static int hsvToRgb(float h, float s, float v) {
        int i = (int) (h * 6);
        float f = h * 6 - i;
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);
        float r, g, b;
        switch (i % 6) {
            case 0:  r = v; g = t; b = p; break;
            case 1:  r = q; g = v; b = p; break;
            case 2:  r = p; g = v; b = t; break;
            case 3:  r = p; g = q; b = v; break;
            case 4:  r = t; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }
        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    private String toLegacy(String miniMessage) {
        return LEGACY.serialize(MM.deserialize(miniMessage));
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
