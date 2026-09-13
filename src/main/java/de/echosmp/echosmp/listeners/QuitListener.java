package de.echosmp.echosmp.listeners;

import de.echosmp.echosmp.PlaytimeManager;
import de.echosmp.echosmp.ScoreboardManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Beim Quit wird die Spielzeit gespeichert und das Scoreboard entfernt.
 */
public class QuitListener implements Listener {

    private final PlaytimeManager playtimeManager;
    private final ScoreboardManager scoreboardManager;

    public QuitListener(PlaytimeManager playtimeManager, ScoreboardManager scoreboardManager) {
        this.playtimeManager = playtimeManager;
        this.scoreboardManager = scoreboardManager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        playtimeManager.handleQuit(event.getPlayer());
        scoreboardManager.removePlayer(event.getPlayer());
    }
}
